package com.eventflow.service;

public interface ConferenceBadgeService {
    byte[] generateSingleBadgePdf(String ticketCode, Long currentUserId);
    byte[] generateBulkBadgesPdf(Long eventId, Long currentUserId);
}
