package com.eventflow.controller;

import com.eventflow.dto.CheckInRequest;
import com.eventflow.dto.LoginRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase10IntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private String organizerToken;
    private User attendee;
    private String attendeeToken;
    private Event testEvent;
    private TicketTier testTier;
    private Ticket testTicket;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        organizer = new User("org_" + uid + "@phase10.com", passwordEncoder.encode("Password@123"), "Organizer Ten", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase10.com", passwordEncoder.encode("Password@123"), "Attendee Ten", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken("org_" + uid + "@phase10.com", "Password@123");
        attendeeToken = loginAndGetToken("att_" + uid + "@phase10.com", "Password@123");

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Turnstile Expo", "turnstile-" + uid, "Tech expo", "cpu")));
        Venue ven = venueRepository.save(new Venue("Grand Turnstile Arena " + uid, "100 Turnstile Way", "Austin", "TX", "78701", "USA", 5000, false, null));

        testEvent = new Event(
                organizer, cat, ven,
                "Cloud Scale Turnstile 2026 " + uid,
                "cloud-scale-turnstile-" + uid,
                "Live door ops demo",
                "Full description of door ops",
                null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        testTier = ticketTierRepository.save(new TicketTier(testEvent, "Standard Pass", "General Admission", new BigDecimal("49.00"), 500));

        Order order = new Order("ORD-P10-" + uid, attendee, testEvent, new BigDecimal("49.00"), OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        String tCode = "TCK-P10-" + uid.toUpperCase();
        testTicket = new Ticket(order, testTier, tCode, attendee.getFullName(), attendee.getEmail(), "QR-" + tCode);
        testTicket.setStatus(TicketStatus.VALID);
        testTicket = ticketRepository.save(testTicket);
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
    @DisplayName("Phase 10: Door stats returns accurate counts before and after check-in")
    void testDoorStatsAndCheckInFlow() throws Exception {
        // 1. Initial door stats query
        MvcResult initialStatsRes = mockMvc.perform(get("/api/tickets/event/" + testEvent.getId() + "/door-stats")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(testEvent.getId()))
                .andExpect(jsonPath("$.data.totalIssuedTickets").value(1))
                .andExpect(jsonPath("$.data.checkedInCount").value(0))
                .andExpect(jsonPath("$.data.pendingCheckInCount").value(1))
                .andExpect(jsonPath("$.data.checkInPercentage").value(0.0))
                .andReturn();

        // 2. Perform check-in using ticket code
        CheckInRequest checkInReq = new CheckInRequest();
        checkInReq.setTicketCode(testTicket.getTicketCode());

        mockMvc.perform(post("/api/tickets/check-in")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.ticket.status").value("CHECKED_IN"));

        // 3. Query door stats after check-in
        mockMvc.perform(get("/api/tickets/event/" + testEvent.getId() + "/door-stats")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkedInCount").value(1))
                .andExpect(jsonPath("$.data.pendingCheckInCount").value(0))
                .andExpect(jsonPath("$.data.checkInPercentage").value(100.0))
                .andExpect(jsonPath("$.data.arrivalsLast15Min").value(1))
                .andExpect(jsonPath("$.data.recentCheckIns[0].ticketCode").value(testTicket.getTicketCode()));
    }
}
