package com.eventflow.controller;

import com.eventflow.dto.*;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase5IntegrationTest {

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

    private User attendeeUser;
    private String attendeeToken;
    private Event publishedEvent;
    private TicketTier tierVip;

    @BeforeEach
    void setUp() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 6);

        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();

        User organizer = new User("org_" + uid + "@phase5.com", passwordEncoder.encode("Password@123"), "Organizer " + uid, "555-0101");
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);

        attendeeUser = new User("att_" + uid + "@phase5.com", passwordEncoder.encode("Password@123"), "Attendee " + uid, "555-0202");
        attendeeUser.setRoles(Collections.singleton(attendeeRole));
        attendeeUser.setStatus(UserStatus.ACTIVE);
        attendeeUser = userRepository.save(attendeeUser);

        // Login Attendee to obtain valid JWT token
        LoginRequest attLogin = new LoginRequest("att_" + uid + "@phase5.com", "Password@123");
        MvcResult attRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attLogin)))
                .andExpect(status().isOk())
                .andReturn();
        attendeeToken = extractToken(attRes);

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Tech Innovation", "tech-" + uid, "Desc", "cpu")));

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() ->
                venueRepository.save(new Venue("Grand Hall " + uid, "100 Innovation Way", "Austin", "TX", "78701", "USA", 2000, false, null)));

        publishedEvent = new Event(
                organizer, cat, venue,
                "Cloud Concurrency Summit " + uid,
                "cloud-summit-" + uid,
                "High scale distributed systems conference",
                "Full description",
                null,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(12)
        );
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent = eventRepository.save(publishedEvent);

        tierVip = ticketTierRepository.save(new TicketTier(
                publishedEvent, "VIP All-Access", "Access to keynote and deal rooms",
                BigDecimal.valueOf(100.00), 100
        ));
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Should validate promo code and calculate discount accurately")
    void testValidatePromoCode() throws Exception {
        ValidatePromoCodeRequest validReq = new ValidatePromoCodeRequest("PULSE20", publishedEvent.getId(), BigDecimal.valueOf(100.00));

        mockMvc.perform(post("/api/promo-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(20.00))
                .andExpect(jsonPath("$.data.finalTotal").value(80.00));

        ValidatePromoCodeRequest invalidReq = new ValidatePromoCodeRequest("FAKECODE123", publishedEvent.getId(), BigDecimal.valueOf(100.00));
        mockMvc.perform(post("/api/promo-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
    }

    @Test
    @DisplayName("Should apply promo code to ticket order and reduce total amount")
    void testCreateOrderWithPromoCode() throws Exception {
        CreateOrderItemRequest item = new CreateOrderItemRequest(tierVip.getId(), 2, "Alice Attendee", "alice@test.com");
        CreateOrderRequest orderReq = new CreateOrderRequest(publishedEvent.getId(), List.of(item), "PULSE20");

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalAmount").value(200.00))
                .andExpect(jsonPath("$.data.discountAmount").value(40.00))
                .andExpect(jsonPath("$.data.totalAmount").value(160.00))
                .andExpect(jsonPath("$.data.promoCode").value("PULSE20"))
                .andReturn();

        String resJson = result.getResponse().getContentAsString();
        assertThat(resJson).contains("PULSE20");
    }

    @Test
    @DisplayName("Should submit and retrieve event review with verified attendee badge")
    void testEventReviews() throws Exception {
        // First order a ticket so attendee is verified
        CreateOrderItemRequest item = new CreateOrderItemRequest(tierVip.getId(), 1, attendeeUser.getFullName(), attendeeUser.getEmail());
        CreateOrderRequest orderReq = new CreateOrderRequest(publishedEvent.getId(), List.of(item), null);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated());

        // Now post review
        CreateReviewRequest reviewReq = new CreateReviewRequest(5, "Incredible Experience", "The keynote talks were world class!");

        mockMvc.perform(post("/api/events/" + publishedEvent.getId() + "/reviews")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.verifiedAttendee").value(true));

        // Get reviews
        mockMvc.perform(get("/api/events/" + publishedEvent.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.reviews[0].title").value("Incredible Experience"));
    }

    @Test
    @DisplayName("Should stream valid high-definition PDF pass with magic header bytes")
    void testDownloadTicketPdf() throws Exception {
        CreateOrderItemRequest item = new CreateOrderItemRequest(tierVip.getId(), 1, attendeeUser.getFullName(), attendeeUser.getEmail());
        CreateOrderRequest orderReq = new CreateOrderRequest(publishedEvent.getId(), List.of(item), null);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse orderRes = objectMapper.readValue(
                objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("data").toString(),
                OrderResponse.class
        );
        String ticketCode = orderRes.getTickets().get(0).getTicketCode();

        MvcResult pdfResult = mockMvc.perform(get("/api/tickets/" + ticketCode + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        // Check standard PDF file signature: starts with %PDF-
        String header = new String(pdfBytes, 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }
}
