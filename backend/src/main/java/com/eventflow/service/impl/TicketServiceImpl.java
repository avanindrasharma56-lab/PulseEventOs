package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.TicketStatus;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.NotificationService;
import com.eventflow.service.PromoCodeService;
import com.eventflow.service.QrCodeService;
import com.eventflow.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketTierRepository ticketTierRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final NotificationService notificationService;
    private final com.eventflow.service.EmailService emailService;
    private final PromoCodeService promoCodeService;
    private final SimpMessagingTemplate messagingTemplate;

    public TicketServiceImpl(
            TicketTierRepository ticketTierRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            QrCodeService qrCodeService,
            NotificationService notificationService,
            com.eventflow.service.EmailService emailService,
            PromoCodeService promoCodeService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.ticketTierRepository = ticketTierRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.promoCodeService = promoCodeService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTierResponse> getTiersForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        return ticketTierRepository.findByEventIdAndActiveTrueOrderByPriceAsc(eventId).stream()
                .map(this::mapToTierResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TicketTierResponse createTier(Long eventId, CreateTicketTierRequest request, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        TicketTier tier = new TicketTier(
                event,
                request.getName().trim(),
                request.getDescription(),
                request.getPrice(),
                request.getCapacity()
        );
        tier.setSalesStartTime(request.getSalesStartTime());
        tier.setSalesEndTime(request.getSalesEndTime());

        TicketTier saved = ticketTierRepository.save(tier);
        return mapToTierResponse(saved);
    }

    @Override
    public TicketTierResponse updateTier(Long eventId, Long tierId, CreateTicketTierRequest request, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        TicketTier tier = ticketTierRepository.findByIdAndEventId(tierId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", "id", tierId));

        if (request.getCapacity() < tier.getSoldCount()) {
            throw new BadRequestException("Capacity cannot be reduced below current sold count (" + tier.getSoldCount() + ")");
        }

        tier.setName(request.getName().trim());
        tier.setDescription(request.getDescription());
        tier.setPrice(request.getPrice());
        tier.setCapacity(request.getCapacity());
        tier.setSalesStartTime(request.getSalesStartTime());
        tier.setSalesEndTime(request.getSalesEndTime());

        TicketTier saved = ticketTierRepository.save(tier);
        return mapToTierResponse(saved);
    }

    @Override
    public void deleteTier(Long eventId, Long tierId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        TicketTier tier = ticketTierRepository.findByIdAndEventId(tierId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", "id", tierId));

        if (tier.getSoldCount() > 0) {
            throw new BadRequestException("Cannot delete ticket tier with " + tier.getSoldCount() + " already sold tickets. You can mark it inactive instead.");
        }

        ticketTierRepository.delete(tier);
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, Long currentUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus());
        }

        User buyer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderNumber, buyer, event, BigDecimal.ZERO, OrderStatus.CONFIRMED);
        order.setPaymentReference("SIMULATED-PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

        BigDecimal grandTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (CreateOrderItemRequest itemReq : request.getItems()) {
            // Strict Concurrency Protection: Acquire pessimistic row lock on the ticket tier
            TicketTier tier = ticketTierRepository.findByIdWithLock(itemReq.getTicketTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("TicketTier", "id", itemReq.getTicketTierId()));

            if (!tier.getEvent().getId().equals(event.getId())) {
                throw new BadRequestException("Ticket tier '" + tier.getName() + "' does not belong to this event");
            }

            if (!tier.isActive()) {
                throw new BadRequestException("Ticket tier '" + tier.getName() + "' is currently inactive");
            }

            if (tier.getSalesStartTime() != null && now.isBefore(tier.getSalesStartTime())) {
                throw new BadRequestException("Sales for tier '" + tier.getName() + "' have not started yet");
            }

            if (tier.getSalesEndTime() != null && now.isAfter(tier.getSalesEndTime())) {
                throw new BadRequestException("Sales for tier '" + tier.getName() + "' have concluded");
            }

            // Atomic inventory check
            int available = tier.getCapacity() - tier.getSoldCount();
            if (available < itemReq.getQuantity()) {
                throw new BadRequestException("Sold out: Only " + available + " ticket(s) remaining for tier '" + tier.getName() + "'");
            }

            // Increment sold count atomically
            tier.setSoldCount(tier.getSoldCount() + itemReq.getQuantity());
            ticketTierRepository.save(tier);

            // Add Order Item
            OrderItem orderItem = new OrderItem(order, tier, itemReq.getQuantity(), tier.getPrice());
            order.addItem(orderItem);
            grandTotal = grandTotal.add(orderItem.getSubtotal());

            // Generate individual tickets with QR codes
            for (int i = 0; i < itemReq.getQuantity(); i++) {
                String ticketCode = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + (i + 1);
                String attendeeName = (itemReq.getAttendeeName() != null && !itemReq.getAttendeeName().trim().isEmpty())
                        ? itemReq.getAttendeeName().trim() : buyer.getFullName();
                String attendeeEmail = (itemReq.getAttendeeEmail() != null && !itemReq.getAttendeeEmail().trim().isEmpty())
                        ? itemReq.getAttendeeEmail().trim() : buyer.getEmail();

                // ZXing QR Code Generation containing validation identifier
                String qrData = qrCodeService.generateQrCodeBase64("PULSE-TICKET:" + ticketCode, 260, 260);

                Ticket ticket = new Ticket(order, tier, ticketCode, attendeeName, attendeeEmail, qrData);
                order.addTicket(ticket);
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            PromoCode promo = promoCodeService.getValidPromoCodeOrThrow(request.getPromoCode(), event.getId(), grandTotal);
            if (promo != null) {
                if (promo.getDiscountType() == com.eventflow.entity.enums.DiscountType.PERCENTAGE) {
                    BigDecimal percentage = promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                    discount = grandTotal.multiply(percentage).setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    discount = promo.getDiscountValue().min(grandTotal).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                order.setPromoCode(promo);
                order.setDiscountAmount(discount);
                promoCodeService.incrementPromoUsage(promo.getId());
            }
        }

        order.setTotalAmount(grandTotal.subtract(discount).max(BigDecimal.ZERO));
        Order savedOrder = orderRepository.save(order);

        try {
            int ticketCount = savedOrder.getTickets() != null ? savedOrder.getTickets().size() : 0;
            notificationService.sendNotification(
                    buyer,
                    "Order Confirmed: " + savedOrder.getOrderNumber(),
                    "Your reservation for " + event.getTitle() + " (" + ticketCount + " pass(es)) has been secured.",
                    NotificationType.ORDER_CONFIRMED,
                    savedOrder.getOrderNumber()
            );
            emailService.sendTicketConfirmationEmail(savedOrder);
        } catch (Exception e) {
            log.warn("Post-order notification/email processing exception: {}", e.getMessage(), e);
        }

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long currentUserId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(Long currentUserId) {
        return ticketRepository.findByOrderUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketByCode(String ticketCode, Long currentUserId) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = ticket.getOrder().getUser().getId().equals(currentUserId);
        boolean isOrganizer = ticket.getOrder().getEvent().getOrganizer().getId().equals(currentUserId);

        if (!isOwner && !isOrganizer && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this ticket");
        }

        return mapToTicketResponse(ticket);
    }

    @Override
    public CheckInResponse checkInTicket(String ticketCode, Long verifierUserId) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        Event event = ticket.getOrder().getEvent();
        User verifier = userRepository.findById(verifierUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", verifierUserId));

        boolean isAdmin = verifier.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = event.getOrganizer().getId().equals(verifierUserId);

        if (!isOrganizer && !isAdmin) {
            throw new AccessDeniedException("Only the event organizer or an administrator can verify and check in tickets for this event");
        }

        if (ticket.getStatus() == TicketStatus.CHECKED_IN) {
            throw new BadRequestException("Ticket already checked in on " + ticket.getCheckedInAt() + " for attendee " + ticket.getAttendeeName());
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BadRequestException("Ticket is CANCELLED and invalid for entry");
        }

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);

        try {
            if (saved.getOrder() != null && saved.getOrder().getUser() != null) {
                notificationService.sendNotification(
                        saved.getOrder().getUser(),
                        "Check-In Authorized: " + saved.getTicketCode(),
                        "Your pass for " + event.getTitle() + " has been checked in successfully.",
                        NotificationType.CHECK_IN_SUCCESS,
                        saved.getTicketCode()
                );
            }
        } catch (Exception ignored) {}

        // Broadcast to real-time WebSocket Turnstile Cockpit
        try {
            if (messagingTemplate != null) {
                long totalIssued = ticketRepository.countByTicketTierEventId(event.getId());
                long checkedIn = ticketRepository.countByTicketTierEventIdAndStatus(event.getId(), TicketStatus.CHECKED_IN);
                double pct = totalIssued > 0 ? (checkedIn * 100.0 / totalIssued) : 0.0;
                long vel = ticketRepository.countByTicketTierEventIdAndStatusAndCheckedInAtAfter(
                        event.getId(), TicketStatus.CHECKED_IN, LocalDateTime.now().minusMinutes(15));

                LiveCheckInBroadcastDto broadcast = new LiveCheckInBroadcastDto(
                        saved.getTicketCode(),
                        saved.getAttendeeName(),
                        saved.getAttendeeEmail(),
                        event.getId(),
                        event.getTitle(),
                        saved.getTicketTier() != null ? saved.getTicketTier().getName() : "Standard",
                        saved.getCheckedInAt(),
                        (int) checkedIn,
                        (int) totalIssued,
                        Math.round(pct * 10.0) / 10.0,
                        (int) vel
                );

                messagingTemplate.convertAndSend("/topic/events/" + event.getId() + "/checkins", broadcast);
                messagingTemplate.convertAndSend("/topic/turnstile/live", broadcast);
            }
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket live check-in: {}", ex.getMessage());
        }

        String successMsg = "Check-in successful! Welcome " + saved.getAttendeeName() + " to " + event.getTitle() + " (" + saved.getTicketTier().getName() + ")";
        return new CheckInResponse(true, successMsg, mapToTicketResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getEventAttendeeTickets(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        return ticketRepository.findByTicketTierEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DoorStatsDto getDoorStats(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        long total = ticketRepository.countByTicketTierEventId(eventId);
        long checkedIn = ticketRepository.countByTicketTierEventIdAndStatus(eventId, TicketStatus.CHECKED_IN);
        long pending = Math.max(0, total - checkedIn);
        double pct = total > 0 ? (checkedIn * 100.0 / total) : 0.0;
        long velocity = ticketRepository.countByTicketTierEventIdAndStatusAndCheckedInAtAfter(
                eventId, TicketStatus.CHECKED_IN, LocalDateTime.now().minusMinutes(15));

        List<TicketResponse> recent = ticketRepository.findByTicketTierEventIdOrderByCreatedAtDesc(eventId).stream()
                .filter(t -> t.getStatus() == TicketStatus.CHECKED_IN)
                .limit(10)
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());

        DoorStatsDto stats = new DoorStatsDto();
        stats.setEventId(event.getId());
        stats.setEventTitle(event.getTitle());
        stats.setTotalIssuedTickets((int) total);
        stats.setCheckedInCount((int) checkedIn);
        stats.setPendingCheckInCount((int) pending);
        stats.setCheckInPercentage(Math.round(pct * 10.0) / 10.0);
        stats.setArrivalsLast15Min((int) velocity);
        stats.setRecentCheckIns(recent);

        return stats;
    }

    private void validateOrganizerOrAdmin(Event event, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!event.getOrganizer().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to manage tickets for this event");
        }
    }

    private TicketTierResponse mapToTierResponse(TicketTier tier) {
        return new TicketTierResponse(
                tier.getId(),
                tier.getEvent().getId(),
                tier.getName(),
                tier.getDescription(),
                tier.getPrice(),
                tier.getCapacity(),
                tier.getSoldCount(),
                tier.isActive(),
                tier.getSalesStartTime(),
                tier.getSalesEndTime()
        );
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setEventId(order.getEvent().getId());
        res.setEventTitle(order.getEvent().getTitle());
        res.setTotalAmount(order.getTotalAmount());
        res.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        res.setOriginalAmount(order.getTotalAmount().add(res.getDiscountAmount()));
        if (order.getPromoCode() != null) {
            res.setPromoCode(order.getPromoCode().getCode());
        }
        res.setStatus(order.getStatus().name());
        res.setCreatedAt(order.getCreatedAt());

        if (order.getTickets() != null) {
            res.setTickets(order.getTickets().stream()
                    .map(this::mapToTicketResponse)
                    .collect(Collectors.toList()));
        }

        return res;
    }

    private TicketResponse mapToTicketResponse(Ticket ticket) {
        TicketResponse res = new TicketResponse();
        res.setId(ticket.getId());
        res.setOrderId(ticket.getOrder().getId());
        res.setOrderNumber(ticket.getOrder().getOrderNumber());
        res.setEventId(ticket.getOrder().getEvent().getId());
        res.setEventTitle(ticket.getOrder().getEvent().getTitle());
        res.setEventSlug(ticket.getOrder().getEvent().getSlug());
        res.setEventStartTime(ticket.getOrder().getEvent().getStartTime());
        res.setEventEndTime(ticket.getOrder().getEvent().getEndTime());

        Venue venue = ticket.getOrder().getEvent().getVenue();
        if (venue != null) {
            res.setVenueName(venue.getName());
            res.setVenueAddress(venue.getAddress());
            res.setVenueCity(venue.getCity());
            res.setOnline(venue.isOnline());
            res.setStreamUrl(venue.getStreamUrl());
        }

        res.setTierName(ticket.getTicketTier().getName());
        res.setTicketCode(ticket.getTicketCode());
        res.setAttendeeName(ticket.getAttendeeName());
        res.setAttendeeEmail(ticket.getAttendeeEmail());
        res.setQrCodeData(ticket.getQrCodeData());
        res.setStatus(ticket.getStatus().name());
        res.setCheckedInAt(ticket.getCheckedInAt());
        res.setCreatedAt(ticket.getCreatedAt());

        return res;
    }
}
