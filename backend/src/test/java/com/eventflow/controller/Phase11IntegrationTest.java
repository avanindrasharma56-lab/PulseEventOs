package com.eventflow.controller;

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
class Phase11IntegrationTest {

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

        organizer = new User("org_" + uid + "@phase11.com", passwordEncoder.encode("Password@123"), "Organizer Eleven", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase11.com", passwordEncoder.encode("Password@123"), "Attendee Eleven", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken("org_" + uid + "@phase11.com", "Password@123");
        attendeeToken = loginAndGetToken("att_" + uid + "@phase11.com", "Password@123");

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Badge Summit", "badge-" + uid, "Badge summit", "tag")));
        Venue ven = venueRepository.save(new Venue("Badge Hall " + uid, "400 Badge Way", "Denver", "CO", "80202", "USA", 5000, false, null));

        testEvent = new Event(
                organizer, cat, ven,
                "Badge & Lanyard Expo " + uid,
                "badge-lanyard-expo-" + uid,
                "Conference badge demo",
                "Full description",
                null,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(11)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        testTier = ticketTierRepository.save(new TicketTier(testEvent, "VIP Access", "VIP Pass with Keynote access", new BigDecimal("199.00"), 100));

        Order order = new Order("ORD-P11-" + uid, attendee, testEvent, new BigDecimal("199.00"), OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        String tCode = "TCK-P11-" + uid.toUpperCase();
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
    @DisplayName("Phase 11: Single attendee badge PDF and Bulk Organizer badge generation")
    void testSingleAndBulkBadgeGeneration() throws Exception {
        // 1. Attendee downloads their own single badge PDF
        MvcResult singleBadgeRes = mockMvc.perform(get("/api/tickets/" + testTicket.getTicketCode() + "/badge.pdf")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] singlePdf = singleBadgeRes.getResponse().getContentAsByteArray();
        assertThat(singlePdf).isNotEmpty();
        // PDF magic bytes "%PDF-"
        assertThat(new String(singlePdf, 0, 5)).isEqualTo("%PDF-");

        // 2. Organizer downloads bulk badges PDF for the whole event
        MvcResult bulkBadgesRes = mockMvc.perform(get("/api/events/" + testEvent.getId() + "/badges.pdf")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] bulkPdf = bulkBadgesRes.getResponse().getContentAsByteArray();
        assertThat(bulkPdf).isNotEmpty();
        assertThat(new String(bulkPdf, 0, 5)).isEqualTo("%PDF-");

        // 3. Regular attendee trying to download bulk event badges must be forbidden (403)
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/badges.pdf")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isForbidden());
    }
}
