package com.eventflow.controller;

import com.eventflow.dto.CertificateClaimRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase14IntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private String organizerToken;
    private User attendee;
    private String attendeeToken;
    private Event testEvent;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        organizer = new User("org_" + uid + "@phase14.com", passwordEncoder.encode("Password@123"), "Organizer Fourteen", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase14.com", passwordEncoder.encode("Password@123"), "Alex Rivera", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken(organizer.getEmail(), "Password@123");
        attendeeToken = loginAndGetToken(attendee.getEmail(), "Password@123");

        EventCategory category = eventCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            EventCategory c = new EventCategory();
            c.setName("Technology");
            c.setSlug("technology-" + UUID.randomUUID());
            return eventCategoryRepository.save(c);
        });

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue();
            v.setName("Pulse Convention Center");
            v.setCity("San Francisco");
            v.setCapacity(2500);
            return venueRepository.save(v);
        });

        testEvent = new Event();
        testEvent.setTitle("Quantum Computing & AI Summit " + uid);
        testEvent.setSlug("quantum-summit-" + uid);
        testEvent.setOrganizer(organizer);
        testEvent.setCategory(category);
        testEvent.setVenue(venue);
        testEvent.setStartTime(LocalDateTime.now().minusDays(2));
        testEvent.setEndTime(LocalDateTime.now().minusDays(1));
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        TicketTier tier = new TicketTier();
        tier.setEvent(testEvent);
        tier.setName("VIP Attendee");
        tier.setPrice(new BigDecimal("299.00"));
        tier.setCapacity(100);
        tier.setSoldCount(1);
        tier = ticketTierRepository.save(tier);

        Order order = new Order();
        order.setUser(attendee);
        order.setEvent(testEvent);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setTotalAmount(new BigDecimal("299.00"));
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        Ticket ticket = new Ticket();
        ticket.setOrder(order);
        ticket.setTicketTier(tier);
        ticket.setTicketCode("TCK-" + UUID.randomUUID().toString().substring(0, 8));
        ticket.setAttendeeName(attendee.getFullName());
        ticket.setAttendeeEmail(attendee.getEmail());
        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticketRepository.save(ticket);
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
    @DisplayName("Phase 14: End-to-end Certificate Claim, PDF Rendering & Verification Flow")
    void testCertificateFlow() throws Exception {
        // 1. Attendee claims Certificate of Completion
        CertificateClaimRequest claimReq = new CertificateClaimRequest(CertificateType.COMPLETION);
        MvcResult claimRes = mockMvc.perform(post("/api/events/" + testEvent.getId() + "/certificates/claim")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateCode").exists())
                .andExpect(jsonPath("$.data.recipientName").value("Alex Rivera"))
                .andExpect(jsonPath("$.data.certificateType").value("COMPLETION"))
                .andReturn();

        JsonNode claimJson = objectMapper.readTree(claimRes.getResponse().getContentAsString());
        String certificateCode = claimJson.get("data").get("certificateCode").asText();
        assertThat(certificateCode).startsWith("CERT-");

        // 2. Download Landscape Certificate PDF
        MvcResult pdfRes = mockMvc.perform(get("/api/certificates/" + certificateCode + "/pdf"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = pdfRes.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertThat(header).isEqualTo("%PDF-");

        // 3. Public Verification Endpoint for Valid Certificate
        mockMvc.perform(get("/api/certificates/verify/" + certificateCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.certificateCode").value(certificateCode))
                .andExpect(jsonPath("$.data.recipientName").value("Alex Rivera"))
                .andExpect(jsonPath("$.data.eventTitle").value(testEvent.getTitle()));

        // 4. Verification Endpoint for Bogus Code
        mockMvc.perform(get("/api/certificates/verify/CERT-INVALID-9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));

        // 5. Query attendee's certificates list
        mockMvc.perform(get("/api/certificates/my-certificates")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].certificateCode").value(certificateCode));

        // 6. Query organizer certificates roster for event
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/certificates")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].certificateCode").value(certificateCode));
    }
}
