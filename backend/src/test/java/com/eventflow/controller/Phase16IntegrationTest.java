package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase16IntegrationTest {

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
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventSessionRepository eventSessionRepository;

    @Autowired
    private SessionRsvpRepository sessionRsvpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private String organizerToken;
    private User attendee1;
    private String attendee1Token;
    private User attendee2;
    private String attendee2Token;

    private Event testEvent;
    private EventSession sessionA;
    private EventSession sessionBConflict;
    private EventSession sessionCAfternoon;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        organizer = new User("org_" + uid + "@phase16.com", passwordEncoder.encode("Password@123"), "Organizer Sixteen", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee1 = new User("att1_" + uid + "@phase16.com", passwordEncoder.encode("Password@123"), "Sophia Chen", "+1556" + uid);
        attendee1.setStatus(UserStatus.ACTIVE);
        attendee1.setRoles(Collections.singleton(attendeeRole));
        attendee1 = userRepository.save(attendee1);

        attendee2 = new User("att2_" + uid + "@phase16.com", passwordEncoder.encode("Password@123"), "Marcus Brody", "+1557" + uid);
        attendee2.setStatus(UserStatus.ACTIVE);
        attendee2.setRoles(Collections.singleton(attendeeRole));
        attendee2 = userRepository.save(attendee2);

        organizerToken = loginAndGetToken(organizer.getEmail(), "Password@123");
        attendee1Token = loginAndGetToken(attendee1.getEmail(), "Password@123");
        attendee2Token = loginAndGetToken(attendee2.getEmail(), "Password@123");

        EventCategory category = eventCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            EventCategory c = new EventCategory();
            c.setName("Cloud & Distributed Systems");
            c.setSlug("cloud-systems-" + UUID.randomUUID());
            return eventCategoryRepository.save(c);
        });

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue();
            v.setName("Silicon Valley Expo Complex");
            v.setCity("San Jose");
            v.setCapacity(3000);
            return venueRepository.save(v);
        });

        testEvent = new Event();
        testEvent.setTitle("High Scale Distributed Systems 2026 " + uid);
        testEvent.setSlug("high-scale-" + uid);
        testEvent.setOrganizer(organizer);
        testEvent.setCategory(category);
        testEvent.setVenue(venue);
        testEvent.setStartTime(LocalDateTime.now().plusDays(3));
        testEvent.setEndTime(LocalDateTime.now().plusDays(4));
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        TicketTier tier = new TicketTier();
        tier.setEvent(testEvent);
        tier.setName("All-Access Conference Pass");
        tier.setPrice(new BigDecimal("499.00"));
        tier.setCapacity(500);
        tier.setSoldCount(2);
        tier = ticketTierRepository.save(tier);

        // Assign confirmed tickets to both attendees
        createTicketForUser(attendee1, testEvent, tier);
        createTicketForUser(attendee2, testEvent, tier);

        LocalDateTime baseDate = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // Session A: 10:00 - 11:30 in Room 101, Capacity: 1
        sessionA = new EventSession(
                testEvent, null,
                "Deep Dive into Distributed KV Stores",
                "Raft consensus and storage engines",
                "Room 101", "Core Infrastructure",
                SessionType.WORKSHOP,
                baseDate,
                baseDate.plusMinutes(90),
                1
        );
        sessionA = eventSessionRepository.save(sessionA);

        // Session B: 11:00 - 12:30 in Room 102 (Overlaps with Session A!), Capacity: 50
        sessionBConflict = new EventSession(
                testEvent, null,
                "High-Throughput Kafka Architecture",
                "Partitioning and end-to-end latency",
                "Room 102", "Data Streaming",
                SessionType.BREAKOUT,
                baseDate.plusMinutes(60),
                baseDate.plusMinutes(150),
                50
        );
        sessionBConflict = eventSessionRepository.save(sessionBConflict);

        // Session C: 14:00 - 15:30 in Room 103 (Afternoon, No overlap), Capacity: 50
        sessionCAfternoon = new EventSession(
                testEvent, null,
                "Cloud Native Observability with OpenTelemetry",
                "Distributed tracing at scale",
                "Room 103", "Observability",
                SessionType.BREAKOUT,
                baseDate.plusHours(4),
                baseDate.plusHours(5).plusMinutes(30),
                50
        );
        sessionCAfternoon = eventSessionRepository.save(sessionCAfternoon);
    }

    private void createTicketForUser(User user, Event event, TicketTier tier) {
        Order order = new Order();
        order.setUser(user);
        order.setEvent(event);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setTotalAmount(tier.getPrice());
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        Ticket ticket = new Ticket();
        ticket.setOrder(order);
        ticket.setTicketTier(tier);
        ticket.setTicketCode("TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setAttendeeName(user.getFullName());
        ticket.setAttendeeEmail(user.getEmail());
        ticket.setStatus(TicketStatus.VALID);
        ticketRepository.save(ticket);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Test Phase 16: Reservation, Conflict Resolution, Waitlist Promotion and Heatmap")
    void testBreakoutBookingEngine() throws Exception {
        // 1. Attendee 1 reserves seat in Session A (Capacity: 1)
        mockMvc.perform(post("/api/sessions/" + sessionA.getId() + "/rsvp")
                        .header("Authorization", "Bearer " + attendee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.sessionId").value(sessionA.getId()));

        // 2. Attendee 1 attempts to reserve Session B (11:00-12:30) which overlaps with Session A (10:00-11:30)
        mockMvc.perform(post("/api/sessions/" + sessionBConflict.getId() + "/rsvp")
                        .header("Authorization", "Bearer " + attendee1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Schedule conflict")));

        // 3. Attendee 1 reserves Session C (14:00-15:30, non-overlapping afternoon slot)
        mockMvc.perform(post("/api/sessions/" + sessionCAfternoon.getId() + "/rsvp")
                        .header("Authorization", "Bearer " + attendee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // 4. Attendee 2 attempts to reserve Session A (which is at max capacity 1/1)
        mockMvc.perform(post("/api/sessions/" + sessionA.getId() + "/rsvp")
                        .header("Authorization", "Bearer " + attendee2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.data.waitlistPosition").value(1));

        // 5. Query capacity for Session A
        mockMvc.perform(get("/api/sessions/" + sessionA.getId() + "/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxCapacity").value(1))
                .andExpect(jsonPath("$.data.reservedSeats").value(1))
                .andExpect(jsonPath("$.data.availableSeats").value(0))
                .andExpect(jsonPath("$.data.full").value(true))
                .andExpect(jsonPath("$.data.waitlistCount").value(1));

        // 6. Attendee 1 cancels their confirmed reservation for Session A -> Auto-promotes Attendee 2!
        mockMvc.perform(delete("/api/sessions/" + sessionA.getId() + "/rsvp")
                        .header("Authorization", "Bearer " + attendee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 7. Verify Attendee 2 was auto-promoted to CONFIRMED
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/my-rsvps")
                        .header("Authorization", "Bearer " + attendee2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sessionId").value(sessionA.getId()))
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));

        // 8. Organizer queries room capacity heatmap
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/sessions/heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3));
    }
}
