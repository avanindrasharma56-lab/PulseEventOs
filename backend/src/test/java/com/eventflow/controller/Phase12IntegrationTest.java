package com.eventflow.controller;

import com.eventflow.dto.CreateSponsorRequest;
import com.eventflow.dto.LeadScanRequest;
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
class Phase12IntegrationTest {

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
    private SponsorRepository sponsorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private String organizerToken;
    private User attendee;
    private String attendeeToken;
    private Event testEvent;
    private TicketTier testTier;
    private Ticket testTicket;
    private Sponsor testSponsor;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        organizer = new User("org_" + uid + "@phase12.com", passwordEncoder.encode("Password@123"), "Organizer Twelve", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase12.com", passwordEncoder.encode("Password@123"), "Attendee Twelve", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken("org_" + uid + "@phase12.com", "Password@123");
        attendeeToken = loginAndGetToken("att_" + uid + "@phase12.com", "Password@123");

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Expo Tech", "expo-" + uid, "Tech expo", "cpu")));
        Venue ven = venueRepository.save(new Venue("Convention Floor " + uid, "500 Expo Blvd", "Seattle", "WA", "98101", "USA", 5000, false, null));

        testEvent = new Event(
                organizer, cat, ven,
                "Global Developer Expo " + uid,
                "global-developer-expo-" + uid,
                "Exhibitor showcase",
                "Full description",
                null,
                LocalDateTime.now().plusDays(15),
                LocalDateTime.now().plusDays(16)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);

        testTier = ticketTierRepository.save(new TicketTier(testEvent, "All Access Expo", "Badge pass", new BigDecimal("120.00"), 500));

        Order order = new Order("ORD-P12-" + uid, attendee, testEvent, new BigDecimal("120.00"), OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        String tCode = "TCK-P12-" + uid.toUpperCase();
        testTicket = new Ticket(order, testTier, tCode, attendee.getFullName(), attendee.getEmail(), "QR-" + tCode);
        testTicket.setStatus(TicketStatus.VALID);
        testTicket = ticketRepository.save(testTicket);

        testSponsor = new Sponsor(testEvent, "HyperScale Cloud Inc", SponsorTier.PLATINUM,
                "https://example.com/logo.png", "https://example.com", "Booth #402", "Cloud infra", 1);
        testSponsor = sponsorRepository.save(testSponsor);
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
    @DisplayName("Phase 12: Sponsor Registration, Badge Lead Scanning, and CSV Export")
    void testSponsorsAndLeadRetrievalFlow() throws Exception {
        // 1. Public query for event sponsors
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/sponsors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("HyperScale Cloud Inc"))
                .andExpect(jsonPath("$.data[0].tier").value("PLATINUM"))
                .andExpect(jsonPath("$.data[0].boothNumber").value("Booth #402"));

        // 2. Organizer adds a new GOLD sponsor
        CreateSponsorRequest createReq = new CreateSponsorRequest();
        createReq.setName("VectorDB AI");
        createReq.setTier(SponsorTier.GOLD);
        createReq.setBoothNumber("Booth #108");
        createReq.setWebsiteUrl("https://vectordb.ai");
        createReq.setDisplayOrder(2);

        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/sponsors")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("VectorDB AI"))
                .andExpect(jsonPath("$.data.tier").value("GOLD"));

        // 3. Scan attendee badge at HyperScale booth to capture lead
        LeadScanRequest scanReq = new LeadScanRequest();
        scanReq.setTicketCode(testTicket.getTicketCode());
        scanReq.setNotes("Very interested in high-concurrency cloud clusters.");
        scanReq.setRating(5);

        mockMvc.perform(post("/api/sponsors/" + testSponsor.getId() + "/leads/scan")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scanReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendeeName").value(attendee.getFullName()))
                .andExpect(jsonPath("$.data.attendeeEmail").value(attendee.getEmail()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.notes").value("Very interested in high-concurrency cloud clusters."));

        // 4. Organizer views captured leads
        mockMvc.perform(get("/api/sponsors/" + testSponsor.getId() + "/leads")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ticketCode").value(testTicket.getTicketCode()))
                .andExpect(jsonPath("$.data[0].rating").value(5));

        // 5. Organizer exports leads as CSV
        MvcResult csvRes = mockMvc.perform(get("/api/sponsors/" + testSponsor.getId() + "/leads/export.csv")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andReturn();

        String csvContent = csvRes.getResponse().getContentAsString();
        assertThat(csvContent).contains("Lead ID,Attendee Name,Attendee Email,Ticket Code");
        assertThat(csvContent).contains(attendee.getFullName());
        assertThat(csvContent).contains(attendee.getEmail());
    }
}
