package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.TicketStatus;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketOrderIntegrationTest {

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

    private String organizerToken;
    private String attendeeToken;
    private Long organizerId;
    private Long attendeeId;
    private Long eventId;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        // 1. Create Organizer
        User organizer = new User("org.tickets@test.com", passwordEncoder.encode("OrgPass!123"), "Ticket Organizer", null);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);
        organizerId = organizer.getId();

        // 2. Create Attendee
        User attendee = new User("attendee.tickets@test.com", passwordEncoder.encode("AttPass!123"), "Alice Attendee", null);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee.setStatus(UserStatus.ACTIVE);
        attendee = userRepository.save(attendee);
        attendeeId = attendee.getId();

        // Login Organizer
        LoginRequest orgLogin = new LoginRequest("org.tickets@test.com", "OrgPass!123");
        MvcResult orgRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgLogin)))
                .andExpect(status().isOk())
                .andReturn();
        organizerToken = extractToken(orgRes);

        // Login Attendee
        LoginRequest attLogin = new LoginRequest("attendee.tickets@test.com", "AttPass!123");
        MvcResult attRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attLogin)))
                .andExpect(status().isOk())
                .andReturn();
        attendeeToken = extractToken(attRes);

        // Seed Category & Venue
        EventCategory category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("AI Tech", "ai-tech", "Tech", "Cpu")));
        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() ->
                venueRepository.save(new Venue("Grand Hall", "100 Center", "Austin", "TX", "78701", "USA", 1000, false, null)));

        // Create and Publish Event
        Event event = new Event(
                organizer,
                category,
                venue,
                "Cloud Concurrency Summit 2026",
                "cloud-concurrency-summit-2026",
                "High scale distributed systems conference",
                "Deep dive into transactional concurrency and database row locks",
                null,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(12)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);
        eventId = event.getId();
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Phase 3 Full Lifecycle: Tier Creation -> Ordering -> Capacity Exhaustion -> QR Generation -> Single-Use Check-In")
    void testTicketAndCheckInLifecycle() throws Exception {
        // 1. Organizer creates a limited capacity ticket tier (Capacity: 2)
        CreateTicketTierRequest tierReq = new CreateTicketTierRequest(
                "VIP Founder Pass",
                "Backstage and speaker access",
                new BigDecimal("150.00"),
                2
        );

        MvcResult tierRes = mockMvc.perform(post("/api/events/" + eventId + "/tiers")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tierReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("VIP Founder Pass"))
                .andExpect(jsonPath("$.data.capacity").value(2))
                .andExpect(jsonPath("$.data.availableQuantity").value(2))
                .andReturn();

        Long tierId = objectMapper.readTree(tierRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Unauthenticated user views tiers for the event
        mockMvc.perform(get("/api/events/" + eventId + "/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(tierId));

        // 3. Attendee places order for 2 tickets (purchasing all available inventory)
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(tierId, 2, "Alice Attendee", "attendee.tickets@test.com");
        CreateOrderRequest orderReq = new CreateOrderRequest(eventId, List.of(itemReq));

        MvcResult orderRes = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(300.00))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.tickets.length()").value(2))
                .andExpect(jsonPath("$.data.tickets[0].qrCodeData").isNotEmpty())
                .andReturn();

        String ticket1Code = objectMapper.readTree(orderRes.getResponse().getContentAsString())
                .path("data").path("tickets").get(0).path("ticketCode").asText();

        // 4. Second purchase attempt exceeds capacity -> MUST BE REJECTED (Sold Out)
        CreateOrderItemRequest overbookItem = new CreateOrderItemRequest(tierId, 1);
        CreateOrderRequest overbookOrder = new CreateOrderRequest(eventId, List.of(overbookItem));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overbookOrder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sold out")));

        // 5. Attendee retrieves their passes via /api/tickets/my-tickets
        mockMvc.perform(get("/api/tickets/my-tickets")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].eventTitle").value("Cloud Concurrency Summit 2026"));

        // 6. Organizer checks in Ticket 1 via QR Code / ticket code -> SUCCESS
        CheckInRequest checkInReq = new CheckInRequest(ticket1Code);
        mockMvc.perform(post("/api/tickets/check-in")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticket.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.ticket.checkedInAt").isNotEmpty());

        // 7. Duplicate Check-In Attempt with the SAME ticket -> MUST BE REJECTED (Single-Use Guarantee)
        mockMvc.perform(post("/api/tickets/check-in")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already checked in")));
    }
}
