package com.eventflow.service.impl;

import com.eventflow.dto.CreateSponsorRequest;
import com.eventflow.dto.LeadRetrievalDto;
import com.eventflow.dto.LeadScanRequest;
import com.eventflow.dto.SponsorDto;
import com.eventflow.entity.Event;
import com.eventflow.entity.LeadRetrieval;
import com.eventflow.entity.Sponsor;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.LeadRetrievalRepository;
import com.eventflow.repository.SponsorRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.service.SponsorService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SponsorServiceImpl implements SponsorService {

    private final SponsorRepository sponsorRepository;
    private final LeadRetrievalRepository leadRetrievalRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public SponsorServiceImpl(SponsorRepository sponsorRepository,
                              LeadRetrievalRepository leadRetrievalRepository,
                              EventRepository eventRepository,
                              TicketRepository ticketRepository,
                              UserRepository userRepository) {
        this.sponsorRepository = sponsorRepository;
        this.leadRetrievalRepository = leadRetrievalRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SponsorDto> getSponsorsForEvent(Long eventId) {
        return sponsorRepository.findByEvent_IdOrderByDisplayOrderAsc(eventId).stream()
                .map(this::mapToSponsorDto)
                .collect(Collectors.toList());
    }

    @Override
    public SponsorDto createSponsor(Long eventId, CreateSponsorRequest request, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        Sponsor sponsor = new Sponsor(
                event,
                request.getName(),
                request.getTier(),
                request.getLogoUrl(),
                request.getWebsiteUrl(),
                request.getBoothNumber(),
                request.getDescription(),
                request.getDisplayOrder()
        );

        Sponsor saved = sponsorRepository.save(sponsor);
        return mapToSponsorDto(saved);
    }

    @Override
    public void deleteSponsor(Long eventId, Long sponsorId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", "id", sponsorId));

        sponsorRepository.delete(sponsor);
    }

    @Override
    public LeadRetrievalDto scanAttendeeLead(Long sponsorId, LeadScanRequest request, Long currentUserId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", "id", sponsorId));

        Ticket ticket = ticketRepository.findByTicketCode(request.getTicketCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", request.getTicketCode()));

        if (!ticket.getTicketTier().getEvent().getId().equals(sponsor.getEvent().getId())) {
            throw new BadRequestException("This badge pass belongs to a different conference event.");
        }

        User attendee = ticket.getOrder().getUser();

        Optional<LeadRetrieval> existing = leadRetrievalRepository.findBySponsor_IdAndAttendee_Id(sponsorId, attendee.getId());
        LeadRetrieval lead;
        if (existing.isPresent()) {
            lead = existing.get();
            lead.setNotes(request.getNotes());
            if (request.getRating() != null) {
                lead.setRating(request.getRating());
            }
            lead.setScannedAt(LocalDateTime.now());
        } else {
            lead = new LeadRetrieval(
                    sponsor,
                    attendee,
                    ticket.getTicketCode(),
                    ticket.getAttendeeName(),
                    ticket.getAttendeeEmail(),
                    request.getNotes(),
                    request.getRating()
            );
        }

        LeadRetrieval saved = leadRetrievalRepository.save(lead);
        return mapToLeadDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadRetrievalDto> getSponsorLeads(Long sponsorId, Long currentUserId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", "id", sponsorId));

        validateOrganizerOrAdmin(sponsor.getEvent(), currentUserId);

        return leadRetrievalRepository.findBySponsor_IdOrderByScannedAtDesc(sponsorId).stream()
                .map(this::mapToLeadDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSponsorLeadsCsv(Long sponsorId, Long currentUserId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", "id", sponsorId));

        validateOrganizerOrAdmin(sponsor.getEvent(), currentUserId);

        List<LeadRetrieval> leads = leadRetrievalRepository.findBySponsor_IdOrderByScannedAtDesc(sponsorId);

        StringBuilder sb = new StringBuilder();
        sb.append("Lead ID,Attendee Name,Attendee Email,Ticket Code,Rating (1-5),Notes,Scanned At\n");
        for (LeadRetrieval lead : leads) {
            sb.append(lead.getId()).append(",")
              .append("\"").append(escapeCsv(lead.getAttendeeName())).append("\",")
              .append("\"").append(escapeCsv(lead.getAttendeeEmail())).append("\",")
              .append("\"").append(escapeCsv(lead.getTicketCode())).append("\",")
              .append(lead.getRating()).append(",")
              .append("\"").append(escapeCsv(lead.getNotes() != null ? lead.getNotes() : "")).append("\",")
              .append("\"").append(lead.getScannedAt()).append("\"\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private void validateOrganizerOrAdmin(Event event, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = event.getOrganizer().getId().equals(currentUserId);

        if (!isOrganizer && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to manage sponsors or view exhibitor leads for this event");
        }
    }

    private SponsorDto mapToSponsorDto(Sponsor s) {
        return new SponsorDto(
                s.getId(),
                s.getEvent().getId(),
                s.getName(),
                s.getTier(),
                s.getLogoUrl(),
                s.getWebsiteUrl(),
                s.getBoothNumber(),
                s.getDescription(),
                s.getDisplayOrder()
        );
    }

    private LeadRetrievalDto mapToLeadDto(LeadRetrieval l) {
        return new LeadRetrievalDto(
                l.getId(),
                l.getSponsor().getId(),
                l.getSponsor().getName(),
                l.getAttendee().getId(),
                l.getTicketCode(),
                l.getAttendeeName(),
                l.getAttendeeEmail(),
                l.getNotes(),
                l.getRating(),
                l.getScannedAt()
        );
    }
}
