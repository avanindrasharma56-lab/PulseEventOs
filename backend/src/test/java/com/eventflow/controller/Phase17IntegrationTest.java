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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase17IntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventSessionRepository eventSessionRepository;

    @Autowired
    private SessionRsvpRepository sessionRsvpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User attendeeA;
    private User attendeeB;
    private String tokenA;
    private String tokenB;
    private Event event;
    private EventSession sessionA;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ATTENDEE)));

        // Create Attendee A
        String emailA = "net.a." + UUID.randomUUID() + "@example.com";
        attendeeA = userRepository.save(new User(emailA, passwordEncoder.encode("Pass123!"), "Alice Architect", "1234567890"));
        attendeeA.getRoles().add(attendeeRole);
        userRepository.save(attendeeA);

        // Create Attendee B
        String emailB = "net.b." + UUID.randomUUID() + "@example.com";
        attendeeB = userRepository.save(new User(emailB, passwordEncoder.encode("Pass123!"), "Bob Builder", "1234567891"));
        attendeeB.getRoles().add(attendeeRole);
        userRepository.save(attendeeB);

        EventCategory category = new EventCategory();
        category.setName("B2B Tech " + UUID.randomUUID());
        category.setSlug("b2b-" + UUID.randomUUID());
        category = eventCategoryRepository.save(category);

        Venue venue = new Venue();
        venue.setName("Global Convention Center");
        venue.setAddress("100 Tech Way");
        venue.setCity("San Francisco");
        venue.setCountry("USA");
        venue.setCapacity(5000);
        venue = venueRepository.save(venue);

        event = new Event();
        event.setTitle("Enterprise Cloud & AI Expo 2026");
        event.setSlug("cloud-ai-expo-" + UUID.randomUUID());
        event.setDescription("Premier enterprise networking and breakout conference.");
        event.setCategory(category);
        event.setVenue(venue);
        event.setOrganizer(attendeeA);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartTime(LocalDateTime.now().plusDays(2));
        event.setEndTime(LocalDateTime.now().plusDays(4));
        event = eventRepository.save(event);

        TicketTier tier = new TicketTier();
        tier.setEvent(event);
        tier.setName("All-Access Pass");
        tier.setPrice(new BigDecimal("299.00"));
        tier.setCapacity(500);
        tier.setSoldCount(0);
        tier = ticketTierRepository.save(tier);

        // Issue confirmed tickets to attendee A and attendee B
        Order orderA = new Order();
        orderA.setEvent(event);
        orderA.setUser(attendeeA);
        orderA.setOrderNumber("ORD-NET-A-" + UUID.randomUUID().toString().substring(0, 8));
        orderA.setTotalAmount(new BigDecimal("299.00"));
        orderA.setStatus(OrderStatus.CONFIRMED);
        orderA = orderRepository.save(orderA);

        Ticket ticketA = new Ticket();
        ticketA.setOrder(orderA);
        ticketA.setTicketTier(tier);
        ticketA.setTicketCode("TCK-NET-A-" + UUID.randomUUID().toString().substring(0, 8));
        ticketA.setAttendeeEmail(emailA);
        ticketA.setAttendeeName(attendeeA.getFullName());
        ticketA.setStatus(TicketStatus.VALID);
        ticketRepository.save(ticketA);

        Order orderB = new Order();
        orderB.setEvent(event);
        orderB.setUser(attendeeB);
        orderB.setOrderNumber("ORD-NET-B-" + UUID.randomUUID().toString().substring(0, 8));
        orderB.setTotalAmount(new BigDecimal("299.00"));
        orderB.setStatus(OrderStatus.CONFIRMED);
        orderB = orderRepository.save(orderB);

        Ticket ticketB = new Ticket();
        ticketB.setOrder(orderB);
        ticketB.setTicketTier(tier);
        ticketB.setTicketCode("TCK-NET-B-" + UUID.randomUUID().toString().substring(0, 8));
        ticketB.setAttendeeEmail(emailB);
        ticketB.setAttendeeName(attendeeB.getFullName());
        ticketB.setStatus(TicketStatus.VALID);
        ticketRepository.save(ticketB);

        // Create a breakout session for Attendee A at Day 2 15:00 - 16:00
        sessionA = new EventSession();
        sessionA.setEvent(event);
        sessionA.setTitle("Kubernetes Scale Deep Dive");
        sessionA.setRoom("Breakout Room A");
        sessionA.setTrack("Cloud Infrastructure");
        sessionA.setSessionType(SessionType.BREAKOUT);
        sessionA.setStartTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0));
        sessionA.setEndTime(LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0));
        sessionA.setMaxCapacity(50);
        sessionA.setReservedSeats(0);
        sessionA = eventSessionRepository.save(sessionA);

        // Login to get tokens
        tokenA = obtainToken(emailA, "Pass123!");
        tokenB = obtainToken(emailB, "Pass123!");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Phase 17: Complete 1-on-1 B2B Networking Hub, Profile Matchmaking & Meeting Workflow")
    void testNetworkingHubAndMeetingWorkflow() throws Exception {
        Long eventId = event.getId();

        // 1. Create Networking Profile for Attendee A
        UpsertNetworkingProfileRequest profileReqA = new UpsertNetworkingProfileRequest(
                "Lead Infrastructure Architect @ CloudCorp",
                "CloudCorp",
                "Principal Architect",
                "Cloud Native, Kubernetes, Distributed Systems",
                "Seeking enterprise partners and distributed systems leads",
                true
        );

        mockMvc.perform(post("/api/events/" + eventId + "/networking/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline").value("Lead Infrastructure Architect @ CloudCorp"))
                .andExpect(jsonPath("$.data.company").value("CloudCorp"))
                .andExpect(jsonPath("$.data.isAvailable").value(true));

        // 2. Create Networking Profile for Attendee B
        UpsertNetworkingProfileRequest profileReqB = new UpsertNetworkingProfileRequest(
                "VP of Engineering @ DataScale",
                "DataScale",
                "VP Engineering",
                "Big Data, AI, Kubernetes, Cloud Native",
                "Looking to meet cloud infrastructure leaders",
                true
        );

        mockMvc.perform(post("/api/events/" + eventId + "/networking/profile")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline").value("VP of Engineering @ DataScale"));

        // 3. Search Networking Directory
        mockMvc.perform(get("/api/events/" + eventId + "/networking/attendees")
                        .param("query", "CloudCorp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("Alice Architect"));

        mockMvc.perform(get("/api/events/" + eventId + "/networking/attendees")
                        .param("interest", "Kubernetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 4. Request 1-on-1 Meeting from Attendee A to Attendee B at Day 2 10:00 - 10:30
        LocalDateTime meetingStart1 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime meetingEnd1 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(30).withSecond(0).withNano(0);

        RequestMeetingRequest meetingReq1 = new RequestMeetingRequest(
                attendeeB.getId(),
                meetingStart1,
                meetingEnd1,
                null,
                "Let's discuss Kubernetes multi-cluster architecture synergies!"
        );

        MvcResult meetingResult = mockMvc.perform(post("/api/events/" + eventId + "/networking/meetings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.tableNumber").value(1))
                .andExpect(jsonPath("$.data.locationName").value("Networking Hub Table #1"))
                .andReturn();

        JsonNode meetingJson = objectMapper.readTree(meetingResult.getResponse().getContentAsString());
        Long meetingId = meetingJson.path("data").path("id").asLong();
        assertThat(meetingId).isNotNull();

        // 5. Verify Overlapping 1-on-1 Meeting Conflict Rejection (10:15 - 10:45)
        RequestMeetingRequest overlappingMeeting = new RequestMeetingRequest(
                attendeeB.getId(),
                meetingStart1.plusMinutes(15),
                meetingEnd1.plusMinutes(15),
                null,
                "Duplicate slot attempt"
        );

        mockMvc.perform(post("/api/events/" + eventId + "/networking/meetings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingMeeting)))
                .andExpect(status().isBadRequest());

        // 6. Test Cross-Check with Phase 16 Breakout Session RSVP:
        // Attendee A confirms a seat reservation in Breakout Session at 15:00 - 16:00
        SessionRsvp rsvpA = new SessionRsvp();
        rsvpA.setSession(sessionA);
        rsvpA.setUser(attendeeA);
        rsvpA.setRsvpCode("RSVP-TEST-CONFLICT");
        rsvpA.setStatus(SessionRsvpStatus.CONFIRMED);
        rsvpA.setReservedAt(LocalDateTime.now());
        sessionRsvpRepository.save(rsvpA);

        // Attendee B tries to schedule meeting with Attendee A at 15:15 - 15:45 (conflicts with breakout session)
        RequestMeetingRequest sessionConflictReq = new RequestMeetingRequest(
                attendeeA.getId(),
                sessionA.getStartTime().plusMinutes(15),
                sessionA.getStartTime().plusMinutes(45),
                null,
                "Hey Alice, meeting during breakout session?"
        );

        mockMvc.perform(post("/api/events/" + eventId + "/networking/meetings")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionConflictReq)))
                .andExpect(status().isBadRequest());

        // 7. Attendee B accepts the 10:00 meeting
        mockMvc.perform(put("/api/networking/meetings/" + meetingId + "/respond")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("accept", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 8. Query My Meetings for Attendee A and Attendee B
        mockMvc.perform(get("/api/events/" + eventId + "/networking/meetings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data[0].tableNumber").value(1));

        // 9. Attendee A cancels the meeting
        mockMvc.perform(delete("/api/networking/meetings/" + meetingId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
