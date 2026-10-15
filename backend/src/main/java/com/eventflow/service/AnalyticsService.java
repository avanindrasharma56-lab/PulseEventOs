package com.eventflow.service;

import com.eventflow.dto.AdminAnalyticsResponse;
import com.eventflow.dto.EventAnalyticsResponse;
import com.eventflow.dto.OrganizerAnalyticsResponse;

public interface AnalyticsService {

    OrganizerAnalyticsResponse getOrganizerAnalytics(Long organizerUserId);

    EventAnalyticsResponse getEventAnalytics(Long eventId, Long currentUserId);

    AdminAnalyticsResponse getAdminAnalytics();
}
