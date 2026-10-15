package com.eventflow.controller;

import com.eventflow.dto.AwardXpRequest;
import com.eventflow.dto.LoginRequest;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase18IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventCategoryRepository eventCategoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private EventSessionRepository eventSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User attendeeA;
    private User attendeeB;
    private String tokenA;
    private String tokenB;
    private Event event;
    private Sponsor sponsor;
    private EventSession session;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ATTENDEE)));

        String emailA = "gamify.a." + UUID.randomUUID() + "@example.com";
        attendeeA = userRepository.save(new User(emailA, passwordEncoder.encode("Pass123!"), "Alice Player", "1112223334"));
        attendeeA.getRoles().add(attendeeRole);
        userRepository.save(attendeeA);

        String emailB = "gamify.b." + UUID.randomUUID() + "@example.com";
        attendeeB = userRepository.save(new User(emailB, passwordEncoder.encode("Pass123!"), "Bob Runner", "5556667778"));
        attendeeB.getRoles().add(attendeeRole);
        userRepository.save(attendeeB);

        EventCategory category = new EventCategory();
        category.setName("Gaming Summit " + UUID.randomUUID());
        category.setSlug("gaming-" + UUID.randomUUID());
        category = eventCategoryRepository.save(category);

        Venue venue = new Venue();
        venue.setName("Cyber Arena " + UUID.randomUUID());
        venue.setAddress("404 Silicon Way");
        venue.setCity("San Francisco");
        venue.setCapacity(5000);
        venue = venueRepository.save(venue);

        event = new Event();
        event.setTitle("Gamification World Expo " + UUID.randomUUID());
        event.setSlug("gaming-expo-" + UUID.randomUUID());
        event.setDescription("Annual gaming and engagement expo");
        event.setCategory(category);
        event.setVenue(venue);
        event.setOrganizer(attendeeA);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(2));
        event = eventRepository.save(event);

        sponsor = new Sponsor();
        sponsor.setEvent(event);
        sponsor.setName("Tech Titan Booth");
        sponsor.setTier(com.eventflow.entity.enums.SponsorTier.PLATINUM);
        sponsor.setDisplayOrder(1);
        sponsor = sponsorRepository.save(sponsor);

        session = new EventSession();
        session.setEvent(event);
        session.setTitle("Leveling Up Your Career");
        session.setDescription("Keynote session on gamification strategies");
        session.setStartTime(LocalDateTime.now().plusDays(1).withHour(10));
        session.setEndTime(LocalDateTime.now().plusDays(1).withHour(11));
        session.setMaxCapacity(200);
        session = eventSessionRepository.save(session);

        tokenA = authenticateUser(emailA, "Pass123!");
        tokenB = authenticateUser(emailB, "Pass123!");
    }

    private String authenticateUser(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("1. Get initial gamification profile - starts with 0 XP, NOVICE tier")
    void testGetInitialProfile() throws Exception {
        mockMvc.perform(get("/api/events/" + event.getId() + "/gamification/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalXp").value(0))
                .andExpect(jsonPath("$.data.currentTier").value("NOVICE"))
                .andExpect(jsonPath("$.data.nextTierMinXp").value(100))
                .andExpect(jsonPath("$.data.isSwagClaimed").value(false));
    }

    @Test
    @DisplayName("2. Award Turnstile Checkin XP - reaches BRONZE tier and Turnstile Pioneer badge")
    void testTurnstileCheckinAndBadge() throws Exception {
        AwardXpRequest request = new AwardXpRequest(
                GamificationActivityType.TURNSTILE_CHECKIN,
                "GATE_MAIN_01",
                "Gate 1 turnstile admission check-in",
                100
        );

        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/award-xp")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalXp").value(100))
                .andExpect(jsonPath("$.data.currentTier").value("BRONZE"))
                .andExpect(jsonPath("$.data.badges[0].badgeKey").value("first_checkin"));

        // Idempotency verification: same referenceId should not award double XP
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/award-xp")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalXp").value(100));
    }

    @Test
    @DisplayName("3. Scan Sponsor Booth - awards 75 XP and Expo Explorer badge")
    void testScanSponsorBooth() throws Exception {
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/scan-booth/" + sponsor.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalXp").value(75))
                .andExpect(jsonPath("$.data.badges[0].badgeKey").value("booth_scout"));
    }

    @Test
    @DisplayName("4. Check in to Breakout Session - awards 150 XP and Knowledge Seeker badge")
    void testSessionCheckin() throws Exception {
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/session-checkin/" + session.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalXp").value(150))
                .andExpect(jsonPath("$.data.currentTier").value("BRONZE"))
                .andExpect(jsonPath("$.data.badges[0].badgeKey").value("session_master"));
    }

    @Test
    @DisplayName("5. Ascend to GOLD Tier (500+ XP) - unlocks unique Swag Code and Swag claiming")
    void testAscendToGoldAndClaimSwag() throws Exception {
        // Award 550 XP
        AwardXpRequest request = new AwardXpRequest(
                GamificationActivityType.SESSION_ATTENDANCE,
                "KEYNOTE_MAIN",
                "Grand Keynote check-in",
                550
        );

        MvcResult res = mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/award-xp")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalXp").value(550))
                .andExpect(jsonPath("$.data.currentTier").value("GOLD"))
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
        String swagCode = data.get("swagCode").asText();
        assertThat(swagCode).startsWith("SWAG-");

        // Claim Swag Pass
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/claim-swag")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSwagClaimed").value(true));
    }

    @Test
    @DisplayName("6. Public Event Leaderboard - lists ranked attendees with XP and badge counts")
    void testLeaderboardRanking() throws Exception {
        // Attendee A earns 300 XP
        AwardXpRequest reqA = new AwardXpRequest(GamificationActivityType.TURNSTILE_CHECKIN, "A1", "Entry", 300);
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/award-xp")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqA)));

        // Attendee B earns 600 XP (Higher)
        AwardXpRequest reqB = new AwardXpRequest(GamificationActivityType.TURNSTILE_CHECKIN, "B1", "Entry", 600);
        mockMvc.perform(post("/api/events/" + event.getId() + "/gamification/award-xp")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqB)));

        // Get public leaderboard (no auth required)
        mockMvc.perform(get("/api/events/" + event.getId() + "/gamification/leaderboard?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userName").value("Bob Runner"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].totalXp").value(600))
                .andExpect(jsonPath("$.data[1].userName").value("Alice Player"))
                .andExpect(jsonPath("$.data[1].rank").value(2))
                .andExpect(jsonPath("$.data[1].totalXp").value(300));
    }
}
