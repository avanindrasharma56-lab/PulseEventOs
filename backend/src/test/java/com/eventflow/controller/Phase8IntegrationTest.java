package com.eventflow.controller;

import com.eventflow.dto.LoginRequest;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
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
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase8IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EventCategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User attendee1;
    private String token1;
    private User attendee2;
    private String token2;
    private Event testEvent;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        String uid = UUID.randomUUID().toString().substring(0, 6);

        attendee1 = new User("att1_" + uid + "@phase8.com", passwordEncoder.encode("Password@123"), "Attendee One", "+1555" + uid);
        attendee1.setStatus(UserStatus.ACTIVE);
        attendee1.setRoles(Collections.singleton(attendeeRole));
        userRepository.save(attendee1);

        attendee2 = new User("att2_" + uid + "@phase8.com", passwordEncoder.encode("Password@123"), "Attendee Two", "+1556" + uid);
        attendee2.setStatus(UserStatus.ACTIVE);
        attendee2.setRoles(Collections.singleton(attendeeRole));
        userRepository.save(attendee2);

        token1 = loginAndGetToken("att1_" + uid + "@phase8.com", "Password@123");
        token2 = loginAndGetToken("att2_" + uid + "@phase8.com", "Password@123");

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Concert", "concert-" + uid, "Live shows", "music")));
        Venue ven = venueRepository.save(new Venue("Grand Symphony Arena " + uid, "500 Symphony Blvd", "Chicago", "IL", "60601", "USA", 5000, false, null));

        testEvent = new Event(
                attendee1, cat, ven,
                "Symphony of Cloud " + uid,
                "symphony-" + uid,
                "Live performance",
                "Full description",
                null,
                LocalDateTime.now().plusDays(20),
                LocalDateTime.now().plusDays(21)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        ticketTierRepository.save(new TicketTier(testEvent, "VIP Front Stage", "Prime front row seating", new BigDecimal("250.00"), 50));
    }

    private String loginAndGetToken(String email, String password) throws Exception {
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
    @DisplayName("Visual Seat Map retrieval, 5-minute hold locks, and concurrency protection")
    void testSeatMapAndHoldLifecycle() throws Exception {
        // 1. Get Seat Map (should auto-provision sections and seats)
        MvcResult mapRes = mockMvc.perform(get("/api/events/" + testEvent.getId() + "/seat-map")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections").isArray())
                .andReturn();

        JsonNode mapNode = objectMapper.readTree(mapRes.getResponse().getContentAsString());
        long seatId = mapNode.path("data").path("sections").get(0).path("seats").get(0).path("id").asLong();
        String seatCode = mapNode.path("data").path("sections").get(0).path("seats").get(0).path("seatCode").asText();

        assertThat(seatId).isGreaterThan(0);
        assertThat(seatCode).isNotEmpty();

        // 2. Attendee 1 holds seat
        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/seats/" + seatId + "/hold")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seatCode").value(seatCode))
                .andExpect(jsonPath("$.data.status").value("HOLD"))
                .andExpect(jsonPath("$.data.remainingSeconds").isNumber());

        // 3. Attendee 2 tries to hold same seat -> Conflict / Bad Request
        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/seats/" + seatId + "/hold")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 4. Attendee 1 releases hold
        mockMvc.perform(delete("/api/events/" + testEvent.getId() + "/seats/" + seatId + "/hold")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Attendee 2 now acquires hold on newly freed seat
        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/seats/" + seatId + "/hold")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seatCode").value(seatCode));
    }
}
