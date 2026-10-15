package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.NotificationType;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.repository.*;
import com.eventflow.service.NotificationService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsAndNotificationIntegrationTest {

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
    private EventCategoryRepository categoryRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private User attendee;
    private Event testEvent;
    private TicketTier testTier;
    private String organizerToken;
    private String attendeeToken;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        long now = System.currentTimeMillis();
        String orgEmail = "phase4.org." + now + "@test.com";
        String attEmail = "phase4.att." + now + "@test.com";

        organizer = new User(orgEmail, passwordEncoder.encode("OrgPass!123"), "Phase 4 Organizer", null);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);

        attendee = new User(attEmail, passwordEncoder.encode("AttPass!123"), "Phase 4 Attendee", null);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee.setStatus(UserStatus.ACTIVE);
        attendee = userRepository.save(attendee);

        // Login Organizer
        LoginRequest orgLogin = new LoginRequest(orgEmail, "OrgPass!123");
        MvcResult orgRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgLogin)))
                .andExpect(status().isOk())
                .andReturn();
        organizerToken = extractToken(orgRes);

        // Login Attendee
        LoginRequest attLogin = new LoginRequest(attEmail, "AttPass!123");
        MvcResult attRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attLogin)))
                .andExpect(status().isOk())
                .andReturn();
        attendeeToken = extractToken(attRes);

        EventCategory category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("AI Analytics", "ai-analytics", "Analytics", "Activity")));

        testEvent = new Event();
        testEvent.setTitle("Phase 4 Analytics Summit");
        testEvent.setSlug("phase-4-analytics-summit-" + now);
        testEvent.setDescription("In-depth data aggregation");
        testEvent.setShortSummary("Analytics summit");
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setCategory(category);
        testEvent.setStartTime(LocalDateTime.now().plusDays(5));
        testEvent.setEndTime(LocalDateTime.now().plusDays(6));
        testEvent.setOrganizer(organizer);
        testEvent = eventRepository.save(testEvent);

        testTier = new TicketTier(testEvent, "Standard Pass", "Standard access", new BigDecimal("75.00"), 100);
        testTier = ticketTierRepository.save(testTier);
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Notification flow: send notification, check unread count, retrieve list, and mark as read")
    void testNotificationLifecycle() throws Exception {
        // 1. Send test notification
        notificationService.sendNotification(
                attendee,
                "Welcome to PulseEvent",
                "Your account is ready.",
                NotificationType.SYSTEM,
                "REF-001"
        );

        // 2. Verify unread count is 1
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 3. Get notification list
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Welcome to PulseEvent"))
                .andExpect(jsonPath("$.data[0].read").value(false));

        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(attendee.getId());
        Long notifId = notifs.get(0).getId();

        // 4. Mark as read
        mockMvc.perform(put("/api/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk());

        // 5. Verify unread count is now 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("Analytics and Attendee Export: buy ticket, check organizer analytics and export CSV")
    void testAnalyticsAndCsvExport() throws Exception {
        // 1. Attendee purchases ticket
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(testTier.getId(), 2, "Test Person", attendee.getEmail());
        CreateOrderRequest orderReq = new CreateOrderRequest(testEvent.getId(), List.of(itemReq));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(150.0));

        // 2. Organizer fetches analytics overview
        mockMvc.perform(get("/api/analytics/organizer")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.totalTicketsSold").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(150.0))
                .andExpect(jsonPath("$.data.events", hasSize(1)))
                .andExpect(jsonPath("$.data.events[0].tierBreakdowns", hasSize(1)))
                .andExpect(jsonPath("$.data.events[0].tierBreakdowns[0].soldCount").value(2));

        // 3. Organizer exports attendee CSV roster
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/export/attendees")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"attendees-")))
                .andExpect(content().string(containsString("Ticket Code,Attendee Name,Attendee Email,Tier,Price")))
                .andExpect(content().string(containsString("Test Person")))
                .andExpect(content().string(containsString("Standard Pass")));
    }
}
