package com.eventflow.service;

import com.eventflow.dto.*;

import java.util.List;

public interface TicketService {
    List<TicketTierResponse> getTiersForEvent(Long eventId);
    TicketTierResponse createTier(Long eventId, CreateTicketTierRequest request, Long currentUserId);
    TicketTierResponse updateTier(Long eventId, Long tierId, CreateTicketTierRequest request, Long currentUserId);
    void deleteTier(Long eventId, Long tierId, Long currentUserId);

    OrderResponse createOrder(CreateOrderRequest request, Long currentUserId);
    List<OrderResponse> getMyOrders(Long currentUserId);
    List<TicketResponse> getMyTickets(Long currentUserId);
    TicketResponse getTicketByCode(String ticketCode, Long currentUserId);

    CheckInResponse checkInTicket(String ticketCode, Long verifierUserId);
    List<TicketResponse> getEventAttendeeTickets(Long eventId, Long currentUserId);
    DoorStatsDto getDoorStats(Long eventId, Long currentUserId);
}
