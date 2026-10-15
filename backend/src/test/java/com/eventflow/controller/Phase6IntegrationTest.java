package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.RefundStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.TicketStatus;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.entity.enums.WaitlistStatus;
import com.eventflow.repository.*;
import com.eventflow.service.RefundService;
import com.eventflow.service.TicketService;
import com.eventflow.service.WaitlistService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase6IntegrationTest {

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
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private RefundRequestRepository refundRequestRepository;

    @Autowired
    private WaitlistService waitlistService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User attendeeUser;
    private String attendeeToken;
    private User adminUser;
    private String adminToken;
    private Event testEvent;
    private TicketTier soldOutTier;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        // Attendee
        attendeeUser = new User("att_" + uid + "@phase6.com", passwordEncoder.encode("Password@123"), "Attendee " + uid, "+1555" + uid);
        attendeeUser.setStatus(UserStatus.ACTIVE);
        attendeeUser.setRoles(Collections.singleton(attendeeRole));
        userRepository.save(attendeeUser);

        // Admin
        adminUser = new User("admin_" + uid + "@phase6.com", passwordEncoder.encode("AdminPass@123"), "Admin " + uid, "+1555888" + uid);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Collections.singleton(adminRole));
        userRepository.save(adminUser);

        // Logins
        attendeeToken = loginAndGetToken("att_" + uid + "@phase6.com", "Password@123");
        adminToken = loginAndGetToken("admin_" + uid + "@phase6.com", "AdminPass@123");

        // Event & Tier
        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Tech Innovation", "tech-" + uid, "Desc", "cpu")));
        Venue ven = venueRepository.findAll().stream().findFirst().orElseGet(() ->
                venueRepository.save(new Venue("Grand Pavilion " + uid, "100 Market St", "San Francisco", "CA", "94105", "USA", 5000, false, null)));

        testEvent = new Event(
                adminUser, cat, ven,
                "Quantum AI Conference " + uid,
                "quantum-ai-" + uid,
                "Short desc",
                "Full desc",
                null,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(12)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        // Sold out tier: capacity = 10, soldCount = 10 -> availableQuantity = 0
        soldOutTier = new TicketTier(testEvent, "Sold Out VIP", "Full VIP pass", new BigDecimal("150.00"), 10);
        soldOutTier.setSoldCount(10);
        soldOutTier = ticketTierRepository.save(soldOutTier);
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
    @DisplayName("1. Join and leave waitlist for sold-out ticket tier")
    void testJoinAndLeaveWaitlist() throws Exception {
        JoinWaitlistRequest joinReq = new JoinWaitlistRequest(soldOutTier.getId());

        // Join waitlist
        MvcResult joinRes = mockMvc.perform(post("/api/waitlist/join")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.queuePosition").value(1))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andReturn();

        JsonNode joinNode = objectMapper.readTree(joinRes.getResponse().getContentAsString());
        long entryId = joinNode.path("data").path("id").asLong();

        // Check my-entries
        mockMvc.perform(get("/api/waitlist/my-entries")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(entryId))
                .andExpect(jsonPath("$.data[0].tierName").value("Sold Out VIP"));

        // Leave waitlist
        mockMvc.perform(delete("/api/waitlist/" + entryId)
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        WaitlistEntry entry = waitlistEntryRepository.findById(entryId).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
    }

    @Test
    @DisplayName("2. Promote attendee in waitlist and claim offered pass")
    void testPromoteAndClaimWaitlistOffer() throws Exception {
        // Attendee joins waitlist
        WaitlistEntryResponse entry = waitlistService.joinWaitlist(attendeeUser.getId(), soldOutTier.getId());
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.WAITING);

        // Open up 1 slot by decrementing soldCount (capacity=10, soldCount=9 -> available=1)
        soldOutTier.setSoldCount(9);
        ticketTierRepository.save(soldOutTier);

        // Promote next in waitlist
        boolean promoted = waitlistService.promoteNextInWaitlist(soldOutTier.getId());
        assertThat(promoted).isTrue();

        WaitlistEntry updated = waitlistEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
        assertThat(updated.getOfferExpiresAt()).isNotNull();

        // Claim waitlist offer
        mockMvc.perform(post("/api/waitlist/" + entry.getId() + "/claim")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists());

        WaitlistEntry claimed = waitlistEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(WaitlistStatus.CLAIMED);
    }

    @Test
    @DisplayName("3. Request attendee self-service refund and admin approval with inventory restock")
    void testRequestRefundAndAdminApproval() throws Exception {
        // Setup tier with 10 available tickets (capacity=10, soldCount=0)
        soldOutTier.setSoldCount(0);
        ticketTierRepository.save(soldOutTier);

        // 1. Create Order
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setEventId(testEvent.getId());
        orderReq.setItems(List.of(new CreateOrderItemRequest(soldOutTier.getId(), 2, "Alex Mercer", "alex@example.com")));

        OrderResponse orderRes = ticketService.createOrder(orderReq, attendeeUser.getId());
        assertThat(orderRes.getStatus()).isEqualTo("CONFIRMED");

        TicketTier tierAfterOrder = ticketTierRepository.findById(soldOutTier.getId()).orElseThrow();
        assertThat(tierAfterOrder.getAvailableQuantity()).isEqualTo(8);

        // 2. Submit Refund Request
        CreateRefundRequest refundReq = new CreateRefundRequest(orderRes.getId(), "Unable to travel on conference dates");
        MvcResult reqRes = mockMvc.perform(post("/api/refunds/request")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        JsonNode refundNode = objectMapper.readTree(reqRes.getResponse().getContentAsString());
        long refundId = refundNode.path("data").path("id").asLong();

        // 3. Admin Reviews & Approves Refund
        ReviewRefundRequest reviewReq = new ReviewRefundRequest(true, "Full refund approved per policy");
        mockMvc.perform(post("/api/refunds/" + refundId + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));

        // Verify Order status is REFUNDED
        Order order = orderRepository.findById(orderRes.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);

        // Verify Tickets are CANCELLED
        order.getTickets().forEach(t -> assertThat(t.getStatus()).isEqualTo(TicketStatus.CANCELLED));

        // Verify Inventory Restocked (8 + 2 = 10)
        TicketTier tierRestocked = ticketTierRepository.findById(soldOutTier.getId()).orElseThrow();
        assertThat(tierRestocked.getAvailableQuantity()).isEqualTo(10);
    }
}
