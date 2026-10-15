package com.eventflow.service;

import com.eventflow.dto.GamificationProfileDto;
import com.eventflow.dto.LeaderboardEntryDto;
import com.eventflow.entity.GamificationActivityType;

import java.util.List;

public interface GamificationService {

    GamificationProfileDto getProfile(Long eventId, Long userId);

    List<LeaderboardEntryDto> getLeaderboard(Long eventId, int limit);

    GamificationProfileDto awardXp(Long eventId, Long userId, GamificationActivityType activityType, String referenceId, String description, Integer customXp);

    GamificationProfileDto scanBooth(Long eventId, Long userId, Long sponsorId);

    GamificationProfileDto checkInSession(Long eventId, Long userId, Long sessionId);

    GamificationProfileDto claimSwag(Long eventId, Long userId);
}
