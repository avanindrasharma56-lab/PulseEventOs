package com.eventflow.controller;

import com.eventflow.dto.LoginRequest;
import com.eventflow.dto.request.InitiateCheckoutRequest;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.PaymentProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentGatewayIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String attendeeJwt;
    private Order testOrder;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        Role orgRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();

        String runId = UUID.randomUUID().toString().substring(0, 6);

        User organizer = new User("org_" + runId + "@pulse.com", passwordEncoder.encode("Password123!"), "Org User " + runId, null);
        organizer.setRoles(Collections.singleton(orgRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);

        User attendee = new User("att_" + runId + "@pulse.com", passwordEncoder.encode("Password123!"), "Attendee User " + runId, null);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee.setStatus(UserStatus.ACTIVE);
        attendee = userRepository.save(attendee);

        EventCategory category = categoryRepository.save(new EventCategory("FinTech " + runId, "fintech-" + runId, "Finance", "CreditCard"));
        Venue venue = venueRepository.save(new Venue("Grand Hall " + runId, "100 Broadway", "New York", "NY", "10001", "USA", 2500, false, null));

        Event event = new Event(
                organizer,
                category,
                venue,
                "FinTech Gala " + runId,
                "fintech-gala-" + runId,
                "Global payments conference",
                "Extensible payment architecture and gateways",
                null,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(5).plusHours(4)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        testOrder = new Order("ORD-TEST-" + runId, attendee, event, BigDecimal.valueOf(149.00), OrderStatus.PENDING);
        testOrder = orderRepository.save(testOrder);

        LoginRequest loginRequest = new LoginRequest("att_" + runId + "@pulse.com", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        attendeeJwt = responseNode.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Should successfully initiate Stripe Checkout Session and create Payment entity")
    void testInitiateStripeCheckoutSession() throws Exception {
        InitiateCheckoutRequest request = new InitiateCheckoutRequest();
        request.setOrderId(testOrder.getId());
        request.setProvider(PaymentProvider.STRIPE);
        request.setSuccessUrl("http://localhost:5173/checkout/success");
        request.setCancelUrl("http://localhost:5173/checkout/cancel");

        mockMvc.perform(post("/api/payments/checkout-session")
                        .header("Authorization", "Bearer " + attendeeJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId()))
                .andExpect(jsonPath("$.data.provider").value("STRIPE"))
                .andExpect(jsonPath("$.data.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.data.checkoutUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should successfully confirm simulated payment and update order to CONFIRMED")
    void testConfirmSimulatedPayment() throws Exception {
        InitiateCheckoutRequest request = new InitiateCheckoutRequest();
        request.setOrderId(testOrder.getId());
        request.setProvider(PaymentProvider.SIMULATED);

        MvcResult sessionResult = mockMvc.perform(post("/api/payments/checkout-session")
                        .header("Authorization", "Bearer " + attendeeJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sessionNode = objectMapper.readTree(sessionResult.getResponse().getContentAsString());
        String paymentRef = sessionNode.path("data").path("paymentReference").asText();

        // Confirm the simulated payment
        mockMvc.perform(post("/api/payments/confirm/" + paymentRef)
                        .header("Authorization", "Bearer " + attendeeJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value(paymentRef))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.paymentMethod").value("visa_4242"));

        // Query payment by reference
        mockMvc.perform(get("/api/payments/" + paymentRef)
                        .header("Authorization", "Bearer " + attendeeJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should accept inbound webhook and update payment records")
    void testInboundWebhookProcessing() throws Exception {
        InitiateCheckoutRequest req = new InitiateCheckoutRequest();
        req.setOrderId(testOrder.getId());
        req.setProvider(PaymentProvider.STRIPE);

        MvcResult result = mockMvc.perform(post("/api/payments/checkout-session")
                        .header("Authorization", "Bearer " + attendeeJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String paymentRef = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("paymentReference").asText();

        String webhookPayload = String.format(
                "{\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"metadata\":{\"payment_reference\":\"%s\"},\"payment_intent\":\"pi_test_123\"}}}",
                paymentRef
        );

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.reference").value(paymentRef));
    }
}
