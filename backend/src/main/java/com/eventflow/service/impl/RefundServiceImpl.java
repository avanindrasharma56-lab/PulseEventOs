package com.eventflow.service.impl;

import com.eventflow.dto.CreateRefundRequest;
import com.eventflow.dto.RefundRequestResponse;
import com.eventflow.dto.ReviewRefundRequest;
import com.eventflow.entity.Order;
import com.eventflow.entity.OrderItem;
import com.eventflow.entity.RefundRequest;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.TicketTier;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.RefundStatus;
import com.eventflow.entity.enums.TicketStatus;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.OrderRepository;
import com.eventflow.repository.RefundRequestRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.TicketTierRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.service.NotificationService;
import com.eventflow.service.RefundService;
import com.eventflow.service.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketTierRepository ticketTierRepository;
    private final TicketRepository ticketRepository;
    private final WaitlistService waitlistService;
    private final NotificationService notificationService;

    public RefundServiceImpl(
            RefundRequestRepository refundRequestRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            TicketTierRepository ticketTierRepository,
            TicketRepository ticketRepository,
            WaitlistService waitlistService,
            NotificationService notificationService) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.ticketRepository = ticketRepository;
        this.waitlistService = waitlistService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public RefundRequestResponse requestRefund(Long userId, CreateRefundRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have authorization to request a refund for this order");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED orders can be refunded (current status: " + order.getStatus() + ")");
        }

        boolean existingPending = refundRequestRepository.existsByOrderIdAndStatusIn(
                order.getId(), Collections.singletonList(RefundStatus.PENDING));

        if (existingPending) {
            throw new BadRequestException("A refund request is already pending review for this order");
        }

        if (order.getEvent() != null && order.getEvent().getStartTime() != null) {
            if (order.getEvent().getStartTime().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Refunds cannot be requested after the event has started");
            }
        }

        RefundRequest refundRequest = new RefundRequest(order, user, order.getTotalAmount(), request.getReason());
        RefundRequest saved = refundRequestRepository.save(refundRequest);

        log.info("User {} requested refund of ${} for Order #{}", user.getEmail(), order.getTotalAmount(), order.getOrderNumber());

        notificationService.sendNotification(
                user,
                "Refund Request Submitted: Order #" + order.getOrderNumber(),
                "Your refund request for $" + order.getTotalAmount() + " is under review. You will be notified once processed.",
                NotificationType.REFUND_UPDATE,
                saved.getId().toString()
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getMyRefundRequests(Long userId) {
        return refundRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getAllRefundRequests() {
        return refundRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getOrganizerRefundRequests(Long organizerId) {
        return refundRequestRepository.findByOrderEventOrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RefundRequestResponse processRefund(Long reviewerUserId, Long refundRequestId, ReviewRefundRequest review) {
        RefundRequest refund = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundRequestId));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Refund request is not pending (current status: " + refund.getStatus() + ")");
        }

        Order order = refund.getOrder();

        if (Boolean.TRUE.equals(review.getApproved())) {
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setAdminNotes(review.getAdminNotes());
            refund.setProcessedAt(LocalDateTime.now());

            // 1. Update order status
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);

            // 2. Cancel and invalidate tickets
            for (Ticket ticket : order.getTickets()) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
            }

            // 3. Restock inventory with pessimistic locking & notify waitlist
            for (OrderItem item : order.getItems()) {
                if (item.getTicketTier() != null) {
                    TicketTier tier = ticketTierRepository.findByIdWithLock(item.getTicketTier().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket tier not found: " + item.getTicketTier().getId()));

                    int newSoldCount = Math.max(0, tier.getSoldCount() - item.getQuantity());
                    tier.setSoldCount(newSoldCount);
                    ticketTierRepository.save(tier);

                    log.info("Restocked {} passes to tier {}. New available inventory: {}", item.getQuantity(), tier.getName(), tier.getAvailableQuantity());

                    // Promote next waiting attendee in line
                    waitlistService.promoteNextInWaitlist(tier.getId());
                }
            }

            log.info("Refund request #{} APPROVED and PROCESSED for Order #{}", refundRequestId, order.getOrderNumber());

            notificationService.sendNotification(
                refund.getUser(),
                "Refund Approved & Processed",
                "Your refund of $" + refund.getAmount() + " for Order #" + order.getOrderNumber() + " has been approved and issued.",
                NotificationType.REFUND_UPDATE,
                refund.getId().toString()
            );

        } else {
            refund.setStatus(RefundStatus.REJECTED);
            refund.setAdminNotes(review.getAdminNotes());
            refund.setProcessedAt(LocalDateTime.now());

            log.info("Refund request #{} REJECTED for Order #{}", refundRequestId, order.getOrderNumber());

            notificationService.sendNotification(
                refund.getUser(),
                "Refund Request Update",
                "Your refund request for Order #" + order.getOrderNumber() + " was not approved. Note: " + (review.getAdminNotes() != null ? review.getAdminNotes() : "No reason specified"),
                NotificationType.REFUND_UPDATE,
                refund.getId().toString()
            );
        }

        RefundRequest saved = refundRequestRepository.save(refund);
        return mapToResponse(saved);
    }

    private RefundRequestResponse mapToResponse(RefundRequest req) {
        RefundRequestResponse res = new RefundRequestResponse();
        res.setId(req.getId());
        res.setOrderId(req.getOrder().getId());
        res.setOrderNumber(req.getOrder().getOrderNumber());
        res.setUserId(req.getUser().getId());
        res.setUserName(req.getUser().getFullName());
        res.setUserEmail(req.getUser().getEmail());
        if (req.getOrder().getEvent() != null) {
            res.setEventId(req.getOrder().getEvent().getId());
            res.setEventTitle(req.getOrder().getEvent().getTitle());
        }
        res.setAmount(req.getAmount());
        res.setReason(req.getReason());
        res.setStatus(req.getStatus());
        res.setAdminNotes(req.getAdminNotes());
        res.setCreatedAt(req.getCreatedAt());
        res.setProcessedAt(req.getProcessedAt());
        return res;
    }
}
