package com.eventflow.service;

import com.eventflow.dto.OrderResponse;
import com.eventflow.dto.WaitlistEntryResponse;

import java.util.List;

public interface WaitlistService {

    WaitlistEntryResponse joinWaitlist(Long userId, Long ticketTierId);

    void leaveWaitlist(Long userId, Long waitlistEntryId);

    List<WaitlistEntryResponse> getMyWaitlistEntries(Long userId);

    List<WaitlistEntryResponse> getTierWaitlist(Long ticketTierId);

    boolean promoteNextInWaitlist(Long ticketTierId);

    OrderResponse claimWaitlistOffer(Long userId, Long waitlistEntryId, String attendeeName, String attendeeEmail);

    void expireUnclaimedOffers();
}
