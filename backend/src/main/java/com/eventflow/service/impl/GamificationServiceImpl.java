package com.eventflow.service.impl;

import com.eventflow.dto.AttendeeBadgeDto;
import com.eventflow.dto.GamificationActivityLogDto;
import com.eventflow.dto.GamificationProfileDto;
import com.eventflow.dto.LeaderboardEntryDto;
import com.eventflow.entity.*;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.*;
import com.eventflow.service.GamificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationServiceImpl implements GamificationService {

    private final AttendeeGamificationProfileRepository profileRepository;
    private final GamificationActivityLogRepository activityLogRepository;
    private final AttendeeBadgeRepository badgeRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SponsorRepository sponsorRepository;
    private final EventSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GamificationServiceImpl(AttendeeGamificationProfileRepository profileRepository,
                                   GamificationActivityLogRepository activityLogRepository,
                                   AttendeeBadgeRepository badgeRepository,
                                   EventRepository eventRepository,
                                   UserRepository userRepository,
                                   SponsorRepository sponsorRepository,
                                   EventSessionRepository sessionRepository,
                                   @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.profileRepository = profileRepository;
        this.activityLogRepository = activityLogRepository;
        this.badgeRepository = badgeRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.sponsorRepository = sponsorRepository;
        this.sessionRepository = sessionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private AttendeeGamificationProfile getOrCreateEntity(Long eventId, Long userId) {
        return profileRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> {
                    Event event = eventRepository.findById(eventId)
                            .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                    AttendeeGamificationProfile newProfile = new AttendeeGamificationProfile(event, user);
                    return profileRepository.save(newProfile);
                });
    }

    @Override
    @Transactional
    public GamificationProfileDto getProfile(Long eventId, Long userId) {
        AttendeeGamificationProfile profile = getOrCreateEntity(eventId, userId);
        return mapToDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(Long eventId, int limit) {
        int pageSize = limit > 0 ? Math.min(limit, 100) : 20;
        List<AttendeeGamificationProfile> topProfiles = profileRepository.findTopLeaderboardByEventId(eventId, PageRequest.of(0, pageSize));
        List<LeaderboardEntryDto> leaderboard = new ArrayList<>();

        int currentRank = 1;
        for (AttendeeGamificationProfile p : topProfiles) {
            long badgeCount = badgeRepository.findByProfileIdOrderByAwardedAtAsc(p.getId()).size();
            User user = p.getUser();
            String name = p.getUser().getFullName();
            if (name == null || name.trim().isEmpty()) {
                name = user.getEmail();
            }

            LeaderboardEntryDto entry = new LeaderboardEntryDto(
                    currentRank++,
                    user.getId(),
                    name.trim(),
                    user.getEmail(),
                    p.getTotalXp(),
                    p.getCurrentTier(),
                    (int) badgeCount,
                    "Attendee",
                    "EventFlow"
            );
            leaderboard.add(entry);
        }

        return leaderboard;
    }

    @Override
    public GamificationProfileDto awardXp(Long eventId, Long userId, GamificationActivityType activityType,
                                          String referenceId, String description, Integer customXp) {
        AttendeeGamificationProfile profile = getOrCreateEntity(eventId, userId);

        if (referenceId != null && !referenceId.trim().isEmpty()) {
            if (activityLogRepository.existsByProfileIdAndActivityTypeAndReferenceId(profile.getId(), activityType, referenceId)) {
                return mapToDto(profile);
            }
        }

        int xpToAward;
        if (customXp != null && customXp > 0) {
            xpToAward = customXp;
        } else {
            switch (activityType) {
                case TURNSTILE_CHECKIN:
                    xpToAward = 100;
                    break;
                case SPONSOR_BOOTH_SCAN:
                    xpToAward = 75;
                    break;
                case SESSION_ATTENDANCE:
                    xpToAward = 150;
                    break;
                case QA_QUESTION:
                    xpToAward = 30;
                    break;
                case POLL_VOTE:
                    xpToAward = 25;
                    break;
                case NETWORKING_MEETING:
                    xpToAward = 120;
                    break;
                case FEEDBACK_REVIEW:
                    xpToAward = 50;
                    break;
                default:
                    xpToAward = 50;
                    break;
            }
        }

        String logDesc = (description != null && !description.trim().isEmpty()) ? description : ("Earned XP for " + activityType.name());
        GamificationActivityLog log = new GamificationActivityLog(profile, activityType, referenceId, xpToAward, logDesc);
        activityLogRepository.save(log);

        int newTotalXp = profile.getTotalXp() + xpToAward;
        profile.setTotalXp(newTotalXp);

        GamificationTier newTier;
        if (newTotalXp >= 1000) {
            newTier = GamificationTier.PLATINUM;
        } else if (newTotalXp >= 500) {
            newTier = GamificationTier.GOLD;
        } else if (newTotalXp >= 250) {
            newTier = GamificationTier.SILVER;
        } else if (newTotalXp >= 100) {
            newTier = GamificationTier.BRONZE;
        } else {
            newTier = GamificationTier.NOVICE;
        }
        profile.setCurrentTier(newTier);

        if ((newTier == GamificationTier.GOLD || newTier == GamificationTier.PLATINUM) && profile.getSwagCode() == null) {
            String swagCode = "SWAG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            profile.setSwagCode(swagCode);
        }

        awardMilestoneBadges(profile, activityType, newTier);

        AttendeeGamificationProfile savedProfile = profileRepository.save(profile);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/events/" + eventId + "/leaderboard", "LEADERBOARD_UPDATED");
                messagingTemplate.convertAndSend("/topic/events/" + eventId + "/gamification/" + userId, "XP_AWARDED");
            } catch (Exception ignored) {
            }
        }

        return mapToDto(savedProfile);
    }

    private void awardMilestoneBadges(AttendeeGamificationProfile profile, GamificationActivityType activityType, GamificationTier tier) {
        if (activityType == GamificationActivityType.TURNSTILE_CHECKIN && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "first_checkin")) {
            badgeRepository.save(new AttendeeBadge(profile, "first_checkin", "Turnstile Pioneer", "🚀"));
        }
        if (activityType == GamificationActivityType.SPONSOR_BOOTH_SCAN && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "booth_scout")) {
            badgeRepository.save(new AttendeeBadge(profile, "booth_scout", "Expo Explorer", "🔍"));
        }
        if (activityType == GamificationActivityType.SESSION_ATTENDANCE && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "session_master")) {
            badgeRepository.save(new AttendeeBadge(profile, "session_master", "Knowledge Seeker", "🎓"));
        }
        if (activityType == GamificationActivityType.NETWORKING_MEETING && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "networking_pro")) {
            badgeRepository.save(new AttendeeBadge(profile, "networking_pro", "Super Connector", "🤝"));
        }
        if ((activityType == GamificationActivityType.QA_QUESTION || activityType == GamificationActivityType.POLL_VOTE) &&
                !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "voice_of_crowd")) {
            badgeRepository.save(new AttendeeBadge(profile, "voice_of_crowd", "Active Voice", "🗳️"));
        }
        if ((tier == GamificationTier.GOLD || tier == GamificationTier.PLATINUM) && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "tier_gold")) {
            badgeRepository.save(new AttendeeBadge(profile, "tier_gold", "Gold Elite", "🏆"));
        }
        if (tier == GamificationTier.PLATINUM && !badgeRepository.existsByProfileIdAndBadgeKey(profile.getId(), "tier_platinum")) {
            badgeRepository.save(new AttendeeBadge(profile, "tier_platinum", "Eventflow Legend", "👑"));
        }
    }

    @Override
    public GamificationProfileDto scanBooth(Long eventId, Long userId, Long sponsorId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor not found with id: " + sponsorId));
        return awardXp(eventId, userId, GamificationActivityType.SPONSOR_BOOTH_SCAN,
                "SPONSOR_" + sponsorId, "Visited booth: " + sponsor.getName(), 75);
    }

    @Override
    public GamificationProfileDto checkInSession(Long eventId, Long userId, Long sessionId) {
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        return awardXp(eventId, userId, GamificationActivityType.SESSION_ATTENDANCE,
                "SESSION_" + sessionId, "Attended session: " + session.getTitle(), 150);
    }

    @Override
    public GamificationProfileDto claimSwag(Long eventId, Long userId) {
        AttendeeGamificationProfile profile = getOrCreateEntity(eventId, userId);
        if (profile.getSwagCode() == null) {
            throw new IllegalStateException("Attendee has not unlocked the Swag Pass yet (Requires Gold Tier).");
        }
        profile.setIsSwagClaimed(true);
        AttendeeGamificationProfile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    private GamificationProfileDto mapToDto(AttendeeGamificationProfile profile) {
        GamificationProfileDto dto = new GamificationProfileDto();
        dto.setId(profile.getId());
        dto.setEventId(profile.getEvent().getId());
        dto.setUserId(profile.getUser().getId());

        User user = profile.getUser();
        String name = user.getFullName();
        dto.setUserName((name != null && !name.trim().isEmpty()) ? name.trim() : user.getEmail());

        dto.setTotalXp(profile.getTotalXp());
        dto.setCurrentTier(profile.getCurrentTier());
        dto.setSwagCode(profile.getSwagCode());
        dto.setIsSwagClaimed(profile.getIsSwagClaimed());

        long rank = profileRepository.calculateRank(profile.getEvent().getId(), profile.getTotalXp());
        dto.setRankPosition((int) rank);

        switch (profile.getCurrentTier()) {
            case NOVICE:
                dto.setNextTierMinXp(100);
                break;
            case BRONZE:
                dto.setNextTierMinXp(250);
                break;
            case SILVER:
                dto.setNextTierMinXp(500);
                break;
            case GOLD:
                dto.setNextTierMinXp(1000);
                break;
            case PLATINUM:
            default:
                dto.setNextTierMinXp(1000);
                break;
        }

        List<AttendeeBadge> badges = badgeRepository.findByProfileIdOrderByAwardedAtAsc(profile.getId());
        List<AttendeeBadgeDto> badgeDtos = badges.stream()
                .map(b -> new AttendeeBadgeDto(b.getId(), b.getBadgeKey(), b.getBadgeName(), b.getBadgeIcon(), b.getAwardedAt()))
                .collect(Collectors.toList());
        dto.setBadges(badgeDtos);

        List<GamificationActivityLog> logs = activityLogRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId());
        List<GamificationActivityLogDto> logDtos = logs.stream()
                .map(l -> new GamificationActivityLogDto(l.getId(), l.getActivityType(), l.getReferenceId(), l.getXpEarned(), l.getDescription(), l.getCreatedAt()))
                .collect(Collectors.toList());
        dto.setRecentActivities(logDtos);

        return dto;
    }
}
