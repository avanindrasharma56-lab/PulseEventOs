package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.SeatReservationStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.SeatMapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatMapServiceImpl implements SeatMapService {

    private final VenueSeatSectionRepository sectionRepository;
    private final VenueSeatRepository seatRepository;
    private final EventSeatReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final TicketTierRepository ticketTierRepository;
    private final UserRepository userRepository;

    public SeatMapServiceImpl(
            VenueSeatSectionRepository sectionRepository,
            VenueSeatRepository seatRepository,
            EventSeatReservationRepository reservationRepository,
            EventRepository eventRepository,
            VenueRepository venueRepository,
            TicketTierRepository ticketTierRepository,
            UserRepository userRepository) {
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SeatMapDto getEventSeatMap(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        Venue venue = event.getVenue();
        if (venue == null) {
            throw new BadRequestException("This event does not have an assigned venue with reserved seating");
        }

        List<VenueSeatSection> sections = sectionRepository.findByVenueIdOrderByNameAsc(venue.getId());
        if (sections.isEmpty()) {
            // Auto-provision initial seat map for demo & quickstart
            List<TicketTier> tiers = ticketTierRepository.findByEventIdOrderByPriceAsc(eventId);
            TicketTier vipTier = tiers.stream().filter(t -> t.getName().toUpperCase().contains("VIP")).findFirst()
                    .orElse(!tiers.isEmpty() ? tiers.get(0) : null);
            TicketTier genTier = tiers.stream().filter(t -> !t.equals(vipTier)).findFirst()
                    .orElse(vipTier);

            createSectionWithSeats(venue.getId(), "VIP Stage Front", vipTier != null ? vipTier.getId() : null, 3, 8);
            createSectionWithSeats(venue.getId(), "Orchestra Center", genTier != null ? genTier.getId() : null, 4, 10);
            sections = sectionRepository.findByVenueIdOrderByNameAsc(venue.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        List<EventSeatReservation> activeReservations = reservationRepository.findActiveReservationsForEvent(eventId, now);
        Map<Long, EventSeatReservation> reservationMap = activeReservations.stream()
                .collect(Collectors.toMap(r -> r.getSeat().getId(), r -> r, (r1, r2) -> r1));

        int total = 0;
        int available = 0;
        int held = 0;
        int reserved = 0;

        List<SeatSectionDto> sectionDtos = new ArrayList<>();
        for (VenueSeatSection sec : sections) {
            SeatSectionDto secDto = new SeatSectionDto();
            secDto.setId(sec.getId());
            secDto.setName(sec.getName());
            secDto.setRowCount(sec.getRowCount());
            secDto.setSeatsPerRow(sec.getSeatsPerRow());

            if (sec.getTier() != null) {
                secDto.setTierId(sec.getTier().getId());
                secDto.setTierName(sec.getTier().getName());
                secDto.setPrice(sec.getTier().getPrice());
            }

            List<VenueSeat> seats = seatRepository.findBySectionIdOrderByRowLabelAscSeatNumberAsc(sec.getId());
            List<SeatDto> seatDtos = new ArrayList<>();
            for (VenueSeat s : seats) {
                total++;
                EventSeatReservation res = reservationMap.get(s.getId());
                SeatReservationStatus status = SeatReservationStatus.AVAILABLE;
                boolean isMyHold = false;

                if (res != null) {
                    status = res.getStatus();
                    if (status == SeatReservationStatus.HOLD) {
                        held++;
                        if (currentUserId != null && res.getUser().getId().equals(currentUserId)) {
                            isMyHold = true;
                        }
                    } else if (status == SeatReservationStatus.RESERVED) {
                        reserved++;
                    }
                } else {
                    available++;
                }

                seatDtos.add(new SeatDto(s.getId(), sec.getId(), s.getRowLabel(), s.getSeatNumber(), s.getSeatCode(), status, isMyHold));
            }

            secDto.setSeats(seatDtos);
            sectionDtos.add(secDto);
        }

        SeatMapDto mapDto = new SeatMapDto();
        mapDto.setEventId(eventId);
        mapDto.setVenueId(venue.getId());
        mapDto.setVenueName(venue.getName());
        mapDto.setTotalSeats(total);
        mapDto.setAvailableSeats(available);
        mapDto.setHeldSeats(held);
        mapDto.setReservedSeats(reserved);
        mapDto.setSections(sectionDtos);

        return mapDto;
    }

    @Override
    @Transactional
    public HoldSeatResponse holdSeat(Long eventId, Long seatId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        VenueSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        LocalDateTime now = LocalDateTime.now();
        Optional<EventSeatReservation> existingOpt = reservationRepository.findByEventIdAndSeatId(eventId, seatId);

        EventSeatReservation reservation;
        if (existingOpt.isPresent()) {
            reservation = existingOpt.get();
            if (reservation.getStatus() == SeatReservationStatus.RESERVED) {
                throw new BadRequestException("Seat " + seat.getSeatCode() + " is already permanently booked.");
            }
            if (reservation.getStatus() == SeatReservationStatus.HOLD && reservation.getExpiresAt().isAfter(now)) {
                if (!reservation.getUser().getId().equals(userId)) {
                    throw new BadRequestException("Seat " + seat.getSeatCode() + " is currently held by another attendee.");
                }
            }
            // Re-hold or extend
            reservation.setUser(user);
            reservation.setStatus(SeatReservationStatus.HOLD);
            reservation.setExpiresAt(now.plusMinutes(5));
            reservation.setUpdatedAt(now);
        } else {
            reservation = new EventSeatReservation(event, seat, user, now.plusMinutes(5));
        }

        EventSeatReservation saved = reservationRepository.save(reservation);
        long remaining = Duration.between(now, saved.getExpiresAt()).getSeconds();

        return new HoldSeatResponse(
                saved.getId(),
                eventId,
                seat.getId(),
                seat.getSeatCode(),
                saved.getStatus(),
                saved.getExpiresAt(),
                Math.max(0, remaining)
        );
    }

    @Override
    @Transactional
    public void releaseSeatHold(Long eventId, Long seatId, Long userId) {
        Optional<EventSeatReservation> resOpt = reservationRepository.findByEventIdAndSeatId(eventId, seatId);
        if (resOpt.isPresent()) {
            EventSeatReservation res = resOpt.get();
            if (res.getStatus() == SeatReservationStatus.HOLD && res.getUser().getId().equals(userId)) {
                res.setStatus(SeatReservationStatus.RELEASED);
                reservationRepository.save(res);
            }
        }
    }

    @Override
    @Transactional
    public VenueSeatSection createSectionWithSeats(Long venueId, String sectionName, Long tierId, int rows, int seatsPerRow) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));

        TicketTier tier = null;
        if (tierId != null) {
            tier = ticketTierRepository.findById(tierId).orElse(null);
        }

        VenueSeatSection section = new VenueSeatSection(venue, sectionName, tier, rows, seatsPerRow);
        VenueSeatSection savedSection = sectionRepository.save(section);

        List<VenueSeat> seats = new ArrayList<>();
        char rowChar = 'A';
        for (int r = 0; r < rows; r++) {
            String rowLabel = String.valueOf((char) (rowChar + r));
            for (int s = 1; s <= seatsPerRow; s++) {
                String prefix = sectionName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                if (prefix.length() > 4) prefix = prefix.substring(0, 4);
                String code = prefix + "-" + rowLabel + s;
                seats.add(new VenueSeat(savedSection, rowLabel, s, code));
            }
        }

        seatRepository.saveAll(seats);
        return savedSection;
    }
}
