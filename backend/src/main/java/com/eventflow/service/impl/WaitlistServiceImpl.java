package com.eventflow.service.impl;

import com.eventflow.dto.CreateOrderItemRequest;
import com.eventflow.dto.CreateOrderRequest;
import com.eventflow.dto.OrderResponse;
import com.eventflow.dto.WaitlistEntryResponse;
import com.eventflow.entity.TicketTier;
import com.eventflow.entity.User;
import com.eventflow.entity.WaitlistEntry;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.WaitlistStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.TicketTierRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.repository.WaitlistEntryRepository;
import com.eventflow.service.NotificationService;
import com.eventflow.service.TicketService;
import com.eventflow.service.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WaitlistServiceImpl implements WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistServiceImpl.class);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final TicketTierRepository ticketTierRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;

    public WaitlistServiceImpl(
            WaitlistEntryRepository waitlistEntryRepository,
            TicketTierRepository ticketTierRepository,
            UserRepository userRepository,
            TicketService ticketService,
            NotificationService notificationService) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public WaitlistEntryResponse joinWaitlist(Long userId, Long ticketTierId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        TicketTier tier = ticketTierRepository.findById(ticketTierId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket tier not found: " + ticketTierId));

        Integer maxPosition = waitlistEntryRepository.findMaxQueuePositionByTierId(ticketTierId);
        int nextPosition = (maxPosition != null ? maxPosition : 0) + 1;

        Optional<WaitlistEntry> existingOpt = waitlistEntryRepository.findByTicketTierIdAndUserId(ticketTierId, userId);
        WaitlistEntry entry;
        if (existingOpt.isPresent()) {
            entry = existingOpt.get();
            if (entry.getStatus() == WaitlistStatus.WAITING || entry.getStatus() == WaitlistStatus.OFFERED) {
                throw new BadRequestException("You are already on the active waitlist for this ticket tier");
            }
            entry.setStatus(WaitlistStatus.WAITING);
            entry.setQueuePosition(nextPosition);
            entry.setOfferExpiresAt(null);
        } else {
            entry = new WaitlistEntry(tier, user, nextPosition);
        }

        WaitlistEntry saved = waitlistEntryRepository.save(entry);

        log.info("User {} joined waitlist for tier {} at position {}", user.getEmail(), tier.getName(), nextPosition);

        notificationService.sendNotification(
                user,
                "Waitlist Confirmed: " + tier.getName(),
                "You are currently in queue position #" + nextPosition + " for " + tier.getName() + ". You will be notified when a pass opens.",
                NotificationType.SYSTEM,
                saved.getId().toString()
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void leaveWaitlist(Long userId, Long waitlistEntryId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(waitlistEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + waitlistEntryId));

        if (!entry.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to leave this waitlist entry");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistEntryRepository.save(entry);
        log.info("User {} left waitlist entry {}", userId, waitlistEntryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getMyWaitlistEntries(Long userId) {
        return waitlistEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getTierWaitlist(Long ticketTierId) {
        return waitlistEntryRepository.findByTicketTierIdOrderByQueuePositionAsc(ticketTierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean promoteNextInWaitlist(Long ticketTierId) {
        List<WaitlistEntry> waitingEntries = waitlistEntryRepository
                .findByTicketTierIdAndStatusOrderByQueuePositionAsc(ticketTierId, WaitlistStatus.WAITING);

        if (waitingEntries.isEmpty()) {
            return false;
        }

        WaitlistEntry nextEntry = waitingEntries.get(0);
        nextEntry.setStatus(WaitlistStatus.OFFERED);
        nextEntry.setOfferExpiresAt(LocalDateTime.now().plusMinutes(15));
        waitlistEntryRepository.save(nextEntry);

        log.info("Promoted waitlist entry #{} for user {} on tier {}",
                nextEntry.getId(), nextEntry.getUser().getEmail(), nextEntry.getTicketTier().getName());

        notificationService.sendNotification(
                nextEntry.getUser(),
                "Ticket Available! Claim Now: " + nextEntry.getTicketTier().getName(),
                "A reserved ticket pass is now available for you! You have 15 minutes to claim your reservation.",
                NotificationType.WAITLIST_OFFER,
                nextEntry.getId().toString()
        );

        return true;
    }

    @Override
    @Transactional
    public OrderResponse claimWaitlistOffer(Long userId, Long waitlistEntryId, String attendeeName, String attendeeEmail) {
        WaitlistEntry entry = waitlistEntryRepository.findById(waitlistEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found: " + waitlistEntryId));

        if (!entry.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to claim this offer");
        }

        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BadRequestException("Waitlist offer is not in OFFERED status (current: " + entry.getStatus() + ")");
        }

        if (entry.getOfferExpiresAt() != null && entry.getOfferExpiresAt().isBefore(LocalDateTime.now())) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistEntryRepository.save(entry);
            // Promote next in line
            promoteNextInWaitlist(entry.getTicketTier().getId());
            throw new BadRequestException("This ticket reservation offer has expired");
        }

        // Create the order for the attendee
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setEventId(entry.getTicketTier().getEvent().getId());

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                entry.getTicketTier().getId(),
                1,
                attendeeName != null ? attendeeName : entry.getUser().getFullName(),
                attendeeEmail != null ? attendeeEmail : entry.getUser().getEmail()
        );
        orderRequest.setItems(Collections.singletonList(itemReq));

        OrderResponse orderResponse = ticketService.createOrder(orderRequest, userId);

        entry.setStatus(WaitlistStatus.CLAIMED);
        waitlistEntryRepository.save(entry);

        log.info("Waitlist offer #{} claimed successfully by user {}. Order: {}",
                waitlistEntryId, userId, orderResponse.getOrderNumber());

        return orderResponse;
    }

    @Override
    @Transactional
    public void expireUnclaimedOffers() {
        List<WaitlistEntry> expired = waitlistEntryRepository
                .findByStatusAndOfferExpiresAtBefore(WaitlistStatus.OFFERED, LocalDateTime.now());

        for (WaitlistEntry entry : expired) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistEntryRepository.save(entry);
            log.info("Expired unclaimed waitlist offer #{} for user {}", entry.getId(), entry.getUser().getEmail());
            // Immediately offer to the next attendee in the queue
            promoteNextInWaitlist(entry.getTicketTier().getId());
        }
    }

    private WaitlistEntryResponse mapToResponse(WaitlistEntry entry) {
        WaitlistEntryResponse res = new WaitlistEntryResponse();
        res.setId(entry.getId());
        res.setTicketTierId(entry.getTicketTier().getId());
        res.setTierName(entry.getTicketTier().getName());
        res.setTierPrice(entry.getTicketTier().getPrice());
        if (entry.getTicketTier().getEvent() != null) {
            res.setEventId(entry.getTicketTier().getEvent().getId());
            res.setEventTitle(entry.getTicketTier().getEvent().getTitle());
        }
        res.setUserId(entry.getUser().getId());
        res.setUserName(entry.getUser().getFullName());
        res.setUserEmail(entry.getUser().getEmail());
        res.setStatus(entry.getStatus());
        res.setQueuePosition(entry.getQueuePosition());
        res.setOfferExpiresAt(entry.getOfferExpiresAt());
        res.setCreatedAt(entry.getCreatedAt());
        return res;
    }
}
