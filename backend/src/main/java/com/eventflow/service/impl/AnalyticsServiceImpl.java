package com.eventflow.service.impl;

import com.eventflow.dto.*;
import com.eventflow.entity.Event;
import com.eventflow.entity.Order;
import com.eventflow.entity.TicketTier;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.TicketStatus;
import org.springframework.security.access.AccessDeniedException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(
            EventRepository eventRepository,
            TicketTierRepository ticketTierRepository,
            TicketRepository ticketRepository,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OrganizerAnalyticsResponse getOrganizerAnalytics(Long organizerUserId) {
        List<Event> myEvents = eventRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerUserId);

        OrganizerAnalyticsResponse res = new OrganizerAnalyticsResponse();
        res.setTotalEvents(myEvents.size());

        int publishedCount = 0;
        int draftCount = 0;
        int pendingCount = 0;
        int totalSold = 0;
        int totalCap = 0;
        BigDecimal totalRev = BigDecimal.ZERO;
        long totalChecks = 0;

        List<EventAnalyticsResponse> eventAnalyticsList = new ArrayList<>();

        for (Event event : myEvents) {
            if (event.getStatus() == EventStatus.PUBLISHED) publishedCount++;
            else if (event.getStatus() == EventStatus.DRAFT) draftCount++;
            else if (event.getStatus() == EventStatus.PENDING_APPROVAL) pendingCount++;

            EventAnalyticsResponse eventStats = calculateEventAnalytics(event);
            eventAnalyticsList.add(eventStats);

            totalSold += eventStats.getTicketsSold();
            totalCap += eventStats.getTotalCapacity();
            totalRev = totalRev.add(eventStats.getTotalRevenue());
            totalChecks += eventStats.getCheckInCount();
        }

        res.setPublishedEvents(publishedCount);
        res.setDraftEvents(draftCount);
        res.setPendingApprovalEvents(pendingCount);
        res.setTotalTicketsSold(totalSold);
        res.setTotalCapacity(totalCap);
        res.setTotalRevenue(totalRev);
        res.setTotalCheckIns(totalChecks);

        double overallRate = totalSold > 0
                ? BigDecimal.valueOf(((double) totalChecks / totalSold) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        res.setOverallCheckInRate(overallRate);
        res.setEvents(eventAnalyticsList);

        return res;
    }

    @Override
    public EventAnalyticsResponse getEventAnalytics(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = event.getOrganizer().getId().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not authorized to view analytics for this event");
        }

        return calculateEventAnalytics(event);
    }

    @Override
    public AdminAnalyticsResponse getAdminAnalytics() {
        AdminAnalyticsResponse res = new AdminAnalyticsResponse();

        long usersCount = userRepository.count();
        res.setTotalUsers(usersCount);

        List<Event> allEvents = eventRepository.findAll();
        res.setTotalEvents(allEvents.size());

        int publishedCount = 0;
        int pendingCount = 0;
        int totalSold = 0;
        BigDecimal totalPlatformRevenue = BigDecimal.ZERO;
        long totalChecks = ticketRepository.countByStatus(TicketStatus.CHECKED_IN);

        List<EventAnalyticsResponse> allEventStats = new ArrayList<>();

        for (Event event : allEvents) {
            if (event.getStatus() == EventStatus.PUBLISHED) publishedCount++;
            else if (event.getStatus() == EventStatus.PENDING_APPROVAL) pendingCount++;

            EventAnalyticsResponse eventStats = calculateEventAnalytics(event);
            allEventStats.add(eventStats);

            totalSold += eventStats.getTicketsSold();
            totalPlatformRevenue = totalPlatformRevenue.add(eventStats.getTotalRevenue());
        }

        res.setPublishedEvents(publishedCount);
        res.setPendingApprovalEvents(pendingCount);
        res.setTotalTicketsSold(totalSold);
        res.setTotalPlatformRevenue(totalPlatformRevenue);
        res.setTotalCheckIns(totalChecks);

        // Top 5 events by revenue
        List<EventAnalyticsResponse> topEvents = allEventStats.stream()
                .sorted(Comparator.comparing(EventAnalyticsResponse::getTotalRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
        res.setTopEvents(topEvents);

        return res;
    }

    private EventAnalyticsResponse calculateEventAnalytics(Event event) {
        EventAnalyticsResponse res = new EventAnalyticsResponse();
        res.setEventId(event.getId());
        res.setTitle(event.getTitle());
        res.setSlug(event.getSlug());
        res.setStatus(event.getStatus());
        res.setStartTime(event.getStartTime());

        List<TicketTier> tiers = ticketTierRepository.findByEventIdAndActiveTrueOrderByPriceAsc(event.getId());

        int eventSold = 0;
        int eventCapacity = 0;
        BigDecimal eventRevenue = BigDecimal.ZERO;
        List<TierAnalyticsResponse> tierResponses = new ArrayList<>();

        for (TicketTier tier : tiers) {
            eventSold += tier.getSoldCount();
            eventCapacity += tier.getCapacity();

            BigDecimal tierRev = tier.getPrice().multiply(BigDecimal.valueOf(tier.getSoldCount()));
            eventRevenue = eventRevenue.add(tierRev);

            long tierChecks = ticketRepository.countByTicketTierIdAndStatus(tier.getId(), TicketStatus.CHECKED_IN);
            double occRate = tier.getCapacity() > 0
                    ? BigDecimal.valueOf(((double) tier.getSoldCount() / tier.getCapacity()) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            tierResponses.add(new TierAnalyticsResponse(
                    tier.getId(),
                    tier.getName(),
                    tier.getPrice(),
                    tier.getCapacity(),
                    tier.getSoldCount(),
                    tierRev,
                    tierChecks,
                    occRate
            ));
        }

        long checkInCount = ticketRepository.countByTicketTierEventIdAndStatus(event.getId(), TicketStatus.CHECKED_IN);
        double checkInRate = eventSold > 0
                ? BigDecimal.valueOf(((double) checkInCount / eventSold) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        res.setTicketsSold(eventSold);
        res.setTotalCapacity(eventCapacity);
        res.setTotalRevenue(eventRevenue);
        res.setCheckInCount(checkInCount);
        res.setCheckInRate(checkInRate);
        res.setTierBreakdowns(tierResponses);

        // Recent Orders
        List<Order> orders = orderRepository.findByEventIdOrderByCreatedAtDesc(event.getId());
        List<RecentOrderSummaryResponse> recentOrderSummaries = orders.stream()
                .limit(10)
                .map(o -> new RecentOrderSummaryResponse(
                        o.getOrderNumber(),
                        o.getUser().getFullName(),
                        o.getUser().getEmail(),
                        o.getTickets() != null ? o.getTickets().size() : 0,
                        o.getTotalAmount(),
                        o.getStatus(),
                        o.getCreatedAt()
                ))
                .collect(Collectors.toList());
        res.setRecentOrders(recentOrderSummaries);

        return res;
    }
}
