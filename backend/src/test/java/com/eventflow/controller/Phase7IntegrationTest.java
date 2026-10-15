package com.eventflow.controller;

import com.eventflow.dto.*;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.SessionType;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase7IntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private User attendeeUser;
    private String attendeeToken;
    private User adminUser;
    private String adminToken;
    private Event testEvent;

    @BeforeEach
    void setUp() throws Exception {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        attendeeUser = new User("att_" + uid + "@phase7.com", passwordEncoder.encode("Password@123"), "Attendee " + uid, "+1555" + uid);
        attendeeUser.setStatus(UserStatus.ACTIVE);
        attendeeUser.setRoles(Collections.singleton(attendeeRole));
        userRepository.save(attendeeUser);

        adminUser = new User("admin_" + uid + "@phase7.com", passwordEncoder.encode("AdminPass@123"), "Admin " + uid, "+1555" + uid);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Collections.singleton(adminRole));
        userRepository.save(adminUser);

        attendeeToken = loginAndGetToken("att_" + uid + "@phase7.com", "Password@123");
        adminToken = loginAndGetToken("admin_" + uid + "@phase7.com", "AdminPass@123");

        EventCategory cat = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("AI Innovation", "ai-" + uid, "Desc", "cpu")));
        Venue ven = venueRepository.findAll().stream().findFirst().orElseGet(() ->
                venueRepository.save(new Venue("Tech Hub " + uid, "200 Silicon Ave", "San Jose", "CA", "95113", "USA", 3000, false, null)));

        testEvent = new Event(
                adminUser, cat, ven,
                "Global AI & Cloud Summit " + uid,
                "ai-summit-" + uid,
                "Short summary",
                "Full event description",
                null,
                LocalDateTime.now().plusDays(15),
                LocalDateTime.now().plusDays(17)
        );
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);
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
    @DisplayName("1. Register speakers and agenda sessions, verify retrieval")
    void testSpeakersAndAgendaCreation() throws Exception {
        // 1. Register Speaker
        CreateSpeakerRequest speakerReq = new CreateSpeakerRequest(
                testEvent.getId(),
                "Dr. Elena Vance",
                "Chief AI Scientist",
                "NeuralCore Labs",
                "Leading researcher in deep learning architectures",
                "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2",
                "https://twitter.com/elenavance"
        );

        MvcResult spkRes = mockMvc.perform(post("/api/events/" + testEvent.getId() + "/speakers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(speakerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Dr. Elena Vance"))
                .andReturn();

        long speakerId = objectMapper.readTree(spkRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Add Session linked to Speaker
        CreateSessionRequest sessionReq = new CreateSessionRequest();
        sessionReq.setEventId(testEvent.getId());
        sessionReq.setSpeakerId(speakerId);
        sessionReq.setTitle("Keynote: Future of Autonomous AI Architectures");
        sessionReq.setDescription("Deep dive into multi-agent systems and real-time inference.");
        sessionReq.setRoom("Auditorium Alpha");
        sessionReq.setTrack("Artificial Intelligence");
        sessionReq.setSessionType(SessionType.KEYNOTE);
        sessionReq.setStartTime(LocalDateTime.now().plusDays(15).withHour(9).withMinute(0));
        sessionReq.setEndTime(LocalDateTime.now().plusDays(15).withHour(10).withMinute(30));

        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Keynote: Future of Autonomous AI Architectures"))
                .andExpect(jsonPath("$.data.speaker.name").value("Dr. Elena Vance"));

        // 3. Query Public Agenda
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].track").value("Artificial Intelligence"))
                .andExpect(jsonPath("$.data[0].room").value("Auditorium Alpha"));
    }

    @Test
    @DisplayName("2. Attendee personal schedule bookmarking and iCal export stream")
    void testBookmarkAndIcalExport() throws Exception {
        // Setup session
        CreateSessionRequest sessionReq = new CreateSessionRequest();
        sessionReq.setEventId(testEvent.getId());
        sessionReq.setTitle("Zero-Trust Security in High-Concurrency Platforms");
        sessionReq.setDescription("Architecting resilient token validation and write barriers.");
        sessionReq.setRoom("Hall B");
        sessionReq.setTrack("Security");
        sessionReq.setSessionType(SessionType.BREAKOUT);
        sessionReq.setStartTime(LocalDateTime.now().plusDays(15).withHour(11).withMinute(0));
        sessionReq.setEndTime(LocalDateTime.now().plusDays(15).withHour(12).withMinute(0));

        MvcResult sessRes = mockMvc.perform(post("/api/events/" + testEvent.getId() + "/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionReq)))
                .andExpect(status().isOk())
                .andReturn();

        long sessionId = objectMapper.readTree(sessRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 1. Toggle Bookmark for Attendee
        mockMvc.perform(post("/api/events/sessions/" + sessionId + "/bookmark")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        // 2. Fetch Attendee My-Schedule
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/my-schedule")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(sessionId))
                .andExpect(jsonPath("$.data[0].bookmarked").value(true));

        // 3. Export iCal (.ics) Stream
        MvcResult icsRes = mockMvc.perform(get("/api/events/" + testEvent.getId() + "/export/ical")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String icsBody = icsRes.getResponse().getContentAsString();
        assertThat(icsBody).contains("BEGIN:VCALENDAR");
        assertThat(icsBody).contains("BEGIN:VEVENT");
        assertThat(icsBody).contains("SUMMARY:Zero-Trust Security in High-Concurrency Platforms");
        assertThat(icsBody).contains("END:VCALENDAR");
    }
}
