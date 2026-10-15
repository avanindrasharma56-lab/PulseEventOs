package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.SessionRsvpStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.NetworkingMeetingService;
import com.eventflow.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkingMeetingServiceImpl implements NetworkingMeetingService {

    private final AttendeeNetworkingProfileRepository profileRepository;
    private final NetworkingMeetingRepository meetingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final SessionRsvpRepository sessionRsvpRepository;
    private final NotificationService notificationService;

    public NetworkingMeetingServiceImpl(
            AttendeeNetworkingProfileRepository profileRepository,
            NetworkingMeetingRepository meetingRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            SessionRsvpRepository sessionRsvpRepository,
            @Autowired(required = false) NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.meetingRepository = meetingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.sessionRsvpRepository = sessionRsvpRepository;
        this.notificationService = notificationService;
    }

    @Override
    public NetworkingProfileDto upsertProfile(Long eventId, Long userId, UpsertNetworkingProfileRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Verify ticket ownership or organizer/admin status
        boolean isStaff = user.getRoles().stream().anyMatch(r ->
                r.getName() == RoleName.ROLE_ADMIN || r.getName() == RoleName.ROLE_ORGANIZER);

        if (!isStaff && !ticketRepository.existsByOrderUserIdAndTicketTierEventId(userId, eventId)) {
            throw new BadRequestException("You must hold a valid confirmed ticket to create a networking profile for this event.");
        }

        AttendeeNetworkingProfile profile = profileRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> {
                    AttendeeNetworkingProfile newProfile = new AttendeeNetworkingProfile();
                    newProfile.setEvent(event);
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setHeadline(request.getHeadline());
        profile.setCompany(request.getCompany());
        profile.setJobTitle(request.getJobTitle());
        profile.setInterests(request.getInterests());
        profile.setNetworkingGoals(request.getNetworkingGoals());
        profile.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);

        AttendeeNetworkingProfile saved = profileRepository.save(profile);
        return mapToProfileDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkingProfileDto getProfile(Long eventId, Long userId) {
        return profileRepository.findByEventIdAndUserId(eventId, userId)
                .map(this::mapToProfileDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetworkingProfileDto> searchProfiles(Long eventId, String query, String interest) {
        return profileRepository.searchProfiles(eventId, query, interest).stream()
                .map(this::mapToProfileDto)
                .collect(Collectors.toList());
    }

    @Override
    public NetworkingMeetingDto requestMeeting(Long eventId, Long requesterId, RequestMeetingRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", requesterId));

        if (requesterId.equals(request.getRecipientId())) {
            throw new BadRequestException("You cannot schedule a networking meeting with yourself.");
        }

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient User", "id", request.getRecipientId()));

        // 1. Verify ticket status for both parties
        boolean requesterStaff = requester.getRoles().stream().anyMatch(r ->
                r.getName() == RoleName.ROLE_ADMIN || r.getName() == RoleName.ROLE_ORGANIZER);
        if (!requesterStaff && !ticketRepository.existsByOrderUserIdAndTicketTierEventId(requesterId, eventId)) {
            throw new BadRequestException("You must hold a confirmed ticket for this event to schedule meetings.");
        }

        boolean recipientStaff = recipient.getRoles().stream().anyMatch(r ->
                r.getName() == RoleName.ROLE_ADMIN || r.getName() == RoleName.ROLE_ORGANIZER);
        if (!recipientStaff && !ticketRepository.existsByOrderUserIdAndTicketTierEventId(recipient.getId(), eventId)) {
            throw new BadRequestException("The recipient is not a confirmed ticket holder for this event.");
        }

        // 2. Verify recipient profile availability
        AttendeeNetworkingProfile recipientProfile = profileRepository.findByEventIdAndUserId(eventId, recipient.getId())
                .orElseThrow(() -> new BadRequestException("The recipient has not set up an active networking profile."));

        if (!Boolean.TRUE.equals(recipientProfile.getIsAvailable())) {
            throw new BadRequestException("This attendee is currently marked as unavailable for new meeting requests.");
        }

        // 3. Time validation
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BadRequestException("Meeting start time must precede end time.");
        }

        // 4. Time conflict check with existing 1-on-1 meetings
        List<MeetingStatus> activeStatuses = List.of(MeetingStatus.REQUESTED, MeetingStatus.ACCEPTED);
        List<NetworkingMeeting> requesterConflicts = meetingRepository.findConflictingUserMeetings(
                eventId, requesterId, start, end, activeStatuses);
        if (!requesterConflicts.isEmpty()) {
            throw new BadRequestException("You already have a 1-on-1 meeting scheduled during this time window.");
        }

        List<NetworkingMeeting> recipientConflicts = meetingRepository.findConflictingUserMeetings(
                eventId, recipient.getId(), start, end, activeStatuses);
        if (!recipientConflicts.isEmpty()) {
            throw new BadRequestException("The requested attendee already has a 1-on-1 meeting scheduled during this time window.");
        }

        // 5. Cross-check with Phase 16 Breakout Session RSVPs
        List<SessionRsvp> requesterRsvps = sessionRsvpRepository.findByUserIdAndEventIdAndStatus(
                requesterId, eventId, SessionRsvpStatus.CONFIRMED);
        for (SessionRsvp rsvp : requesterRsvps) {
            if (rsvp.getSession().getStartTime().isBefore(end) && rsvp.getSession().getEndTime().isAfter(start)) {
                throw new BadRequestException("Schedule conflict: You have a confirmed breakout session ('" +
                        rsvp.getSession().getTitle() + "') at this time.");
            }
        }

        List<SessionRsvp> recipientRsvps = sessionRsvpRepository.findByUserIdAndEventIdAndStatus(
                recipient.getId(), eventId, SessionRsvpStatus.CONFIRMED);
        for (SessionRsvp rsvp : recipientRsvps) {
            if (rsvp.getSession().getStartTime().isBefore(end) && rsvp.getSession().getEndTime().isAfter(start)) {
                throw new BadRequestException("Schedule conflict: The attendee has a confirmed breakout session ('" +
                        rsvp.getSession().getTitle() + "') at this time.");
            }
        }

        // 6. Dynamic Table Allocation (allocate smallest free table #1..50)
        List<Integer> occupiedTables = meetingRepository.findOccupiedTables(eventId, start, end, activeStatuses);
        int allocatedTable = 1;
        while (occupiedTables.contains(allocatedTable)) {
            allocatedTable++;
        }

        String locationName = request.getLocationName();
        if (locationName == null || locationName.trim().isEmpty()) {
            locationName = "Networking Hub Table #" + allocatedTable;
        }

        NetworkingMeeting meeting = new NetworkingMeeting();
        meeting.setEvent(event);
        meeting.setRequester(requester);
        meeting.setRecipient(recipient);
        meeting.setStartTime(start);
        meeting.setEndTime(end);
        meeting.setTableNumber(allocatedTable);
        meeting.setLocationName(locationName);
        meeting.setMessage(request.getMessage());
        meeting.setStatus(MeetingStatus.REQUESTED);

        NetworkingMeeting saved = meetingRepository.save(meeting);

        // Notify recipient
        if (notificationService != null) {
            notificationService.sendNotification(
                    recipient,
                    "New 1-on-1 Meeting Request",
                    requester.getFullName() + " requested a 1-on-1 meeting at " + locationName,
                    NotificationType.SYSTEM,
                    String.valueOf(saved.getId())
            );
        }

        return mapToMeetingDto(saved);
    }

    @Override
    public NetworkingMeetingDto respondMeeting(Long meetingId, Long userId, boolean accept) {
        NetworkingMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkingMeeting", "id", meetingId));

        if (!meeting.getRecipient().getId().equals(userId)) {
            throw new BadRequestException("Only the invited attendee can respond to this meeting request.");
        }

        if (meeting.getStatus() != MeetingStatus.REQUESTED) {
            throw new BadRequestException("Meeting request cannot be updated because current status is " + meeting.getStatus());
        }

        if (accept) {
            meeting.setStatus(MeetingStatus.ACCEPTED);
            if (notificationService != null) {
                notificationService.sendNotification(
                        meeting.getRequester(),
                        "Meeting Request Accepted",
                        meeting.getRecipient().getFullName() + " accepted your 1-on-1 meeting at " + meeting.getLocationName(),
                        NotificationType.SYSTEM,
                        String.valueOf(meeting.getId())
                );
            }
        } else {
            meeting.setStatus(MeetingStatus.DECLINED);
            if (notificationService != null) {
                notificationService.sendNotification(
                        meeting.getRequester(),
                        "Meeting Request Declined",
                        meeting.getRecipient().getFullName() + " was unable to accept your meeting request.",
                        NotificationType.SYSTEM,
                        String.valueOf(meeting.getId())
                );
            }
        }

        NetworkingMeeting updated = meetingRepository.save(meeting);
        return mapToMeetingDto(updated);
    }

    @Override
    public NetworkingMeetingDto cancelMeeting(Long meetingId, Long userId) {
        NetworkingMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkingMeeting", "id", meetingId));

        boolean isParticipant = meeting.getRequester().getId().equals(userId) || meeting.getRecipient().getId().equals(userId);
        if (!isParticipant) {
            throw new BadRequestException("Only meeting participants can cancel this meeting.");
        }

        if (meeting.getStatus() == MeetingStatus.CANCELLED || meeting.getStatus() == MeetingStatus.DECLINED) {
            throw new BadRequestException("Meeting is already inactive.");
        }

        meeting.setStatus(MeetingStatus.CANCELLED);
        NetworkingMeeting updated = meetingRepository.save(meeting);

        // Notify counterpart
        User otherParty = meeting.getRequester().getId().equals(userId) ? meeting.getRecipient() : meeting.getRequester();
        if (notificationService != null) {
            notificationService.sendNotification(
                    otherParty,
                    "Meeting Cancelled",
                    "A scheduled 1-on-1 meeting was cancelled.",
                    NotificationType.SYSTEM,
                    String.valueOf(meeting.getId())
            );
        }

        return mapToMeetingDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetworkingMeetingDto> getMyMeetings(Long eventId, Long userId) {
        return meetingRepository.findUserMeetingsForEvent(eventId, userId).stream()
                .map(this::mapToMeetingDto)
                .collect(Collectors.toList());
    }

    private NetworkingProfileDto mapToProfileDto(AttendeeNetworkingProfile p) {
        NetworkingProfileDto dto = new NetworkingProfileDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setUserName(p.getUser().getFullName());
        dto.setUserEmail(p.getUser().getEmail());
        dto.setEventId(p.getEvent().getId());
        dto.setHeadline(p.getHeadline());
        dto.setCompany(p.getCompany());
        dto.setJobTitle(p.getJobTitle());
        dto.setInterests(p.getInterests());
        dto.setNetworkingGoals(p.getNetworkingGoals());
        dto.setIsAvailable(p.getIsAvailable());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    private NetworkingMeetingDto mapToMeetingDto(NetworkingMeeting m) {
        AttendeeNetworkingProfile requesterProfile = profileRepository.findByEventIdAndUserId(m.getEvent().getId(), m.getRequester().getId()).orElse(null);
        AttendeeNetworkingProfile recipientProfile = profileRepository.findByEventIdAndUserId(m.getEvent().getId(), m.getRecipient().getId()).orElse(null);

        NetworkingMeetingDto dto = new NetworkingMeetingDto();
        dto.setId(m.getId());
        dto.setEventId(m.getEvent().getId());
        dto.setEventTitle(m.getEvent().getTitle());
        dto.setRequesterId(m.getRequester().getId());
        dto.setRequesterName(m.getRequester().getFullName());
        dto.setRequesterCompany(requesterProfile != null ? requesterProfile.getCompany() : null);
        dto.setRequesterHeadline(requesterProfile != null ? requesterProfile.getHeadline() : null);
        dto.setRecipientId(m.getRecipient().getId());
        dto.setRecipientName(m.getRecipient().getFullName());
        dto.setRecipientCompany(recipientProfile != null ? recipientProfile.getCompany() : null);
        dto.setRecipientHeadline(recipientProfile != null ? recipientProfile.getHeadline() : null);
        dto.setStartTime(m.getStartTime());
        dto.setEndTime(m.getEndTime());
        dto.setTableNumber(m.getTableNumber());
        dto.setLocationName(m.getLocationName());
        dto.setMessage(m.getMessage());
        dto.setStatus(m.getStatus());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setUpdatedAt(m.getUpdatedAt());
        return dto;
    }
}
