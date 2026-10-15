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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase15IntegrationTest {

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
    private SessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private LivePollRepository livePollRepository;

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

        organizer = new User("org_" + uid + "@phase15.com", passwordEncoder.encode("Password@123"), "Organizer Fifteen", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase15.com", passwordEncoder.encode("Password@123"), "Maya Lin", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken(organizer.getEmail(), "Password@123");
        attendeeToken = loginAndGetToken(attendee.getEmail(), "Password@123");

        EventCategory category = eventCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            EventCategory c = new EventCategory();
            c.setName("Technology");
            c.setSlug("tech-" + UUID.randomUUID());
            return eventCategoryRepository.save(c);
        });

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue();
            v.setName("Grand Interactive Arena");
            v.setCity("Seattle");
            v.setCapacity(1800);
            return venueRepository.save(v);
        });

        testEvent = new Event();
        testEvent.setTitle("AI & Interactive Realtime Conference " + uid);
        testEvent.setSlug("ai-interactive-conf-" + uid);
        testEvent.setOrganizer(organizer);
        testEvent.setCategory(category);
        testEvent.setVenue(venue);
        testEvent.setStartTime(LocalDateTime.now().plusDays(1));
        testEvent.setEndTime(LocalDateTime.now().plusDays(2));
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent = eventRepository.save(testEvent);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Test Phase 15: Full Lifecycle of Live Q&A (Ask, Upvote, Pin, Answer)")
    void testLiveQALifecycle() throws Exception {
        // 1. Attendee submits a question
        QuestionSubmitRequest submitReq = new QuestionSubmitRequest(
                "How does the EventFlow real-time WebSocket broker handle 10k concurrent upvotes?",
                false,
                null
        );

        MvcResult submitResult = mockMvc.perform(post("/api/events/" + testEvent.getId() + "/qa")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionText").value(submitReq.getQuestionText()))
                .andExpect(jsonPath("$.data.upvotes").value(0))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        JsonNode submitNode = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        long questionId = submitNode.path("data").path("id").asLong();

        // 2. Attendee upvotes their question
        mockMvc.perform(post("/api/qa/" + questionId + "/upvote")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.upvotes").value(1))
                .andExpect(jsonPath("$.data.userHasUpvoted").value(true));

        // 3. Organizer toggles question status to PINNED
        QuestionStatusUpdateRequest pinReq = new QuestionStatusUpdateRequest(QuestionStatus.PINNED);
        mockMvc.perform(put("/api/qa/" + questionId + "/status")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pinReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PINNED"));

        // 4. Retrieve questions list as attendee and verify order and upvote state
        MvcResult listResult = mockMvc.perform(get("/api/events/" + testEvent.getId() + "/qa")
                        .header("Authorization", "Bearer " + attendeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(questionId))
                .andExpect(jsonPath("$.data[0].userHasUpvoted").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PINNED"))
                .andReturn();

        // 5. Organizer marks question as ANSWERED
        QuestionStatusUpdateRequest answerReq = new QuestionStatusUpdateRequest(QuestionStatus.ANSWERED);
        mockMvc.perform(put("/api/qa/" + questionId + "/status")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANSWERED"));
    }

    @Test
    @DisplayName("Test Phase 15: Full Lifecycle of Live Polling (Create, Vote, Update Status)")
    void testLivePollLifecycle() throws Exception {
        // 1. Organizer creates a poll
        CreatePollRequest pollReq = new CreatePollRequest(
                "What is your primary cloud architecture for high-concurrency event apps?",
                Arrays.asList("Kubernetes / Cloud Run", "AWS ECS / Lambda", "Self-hosted bare metal", "Hybrid Edge")
        );

        MvcResult createResult = mockMvc.perform(post("/api/events/" + testEvent.getId() + "/polls")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pollReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.question").value(pollReq.getQuestion()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.options.length()").value(4))
                .andExpect(jsonPath("$.data.totalVotes").value(0))
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long pollId = createNode.path("data").path("id").asLong();
        long firstOptionId = createNode.path("data").path("options").get(0).path("id").asLong();

        // 2. Attendee casts a vote for Option 0
        PollVoteRequest voteReq = new PollVoteRequest(firstOptionId);
        mockMvc.perform(post("/api/polls/" + pollId + "/vote")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVotes").value(1))
                .andExpect(jsonPath("$.data.hasVoted").value(true))
                .andExpect(jsonPath("$.data.userVotedOptionId").value(firstOptionId))
                .andExpect(jsonPath("$.data.options[0].voteCount").value(1))
                .andExpect(jsonPath("$.data.options[0].percentage").value(100.0));

        // 3. Organizer closes the poll
        PollStatusUpdateRequest closeReq = new PollStatusUpdateRequest(PollStatus.CLOSED);
        mockMvc.perform(put("/api/polls/" + pollId + "/status")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 4. Public unauthenticated user views poll results
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(pollId))
                .andExpect(jsonPath("$.data[0].totalVotes").value(1))
                .andExpect(jsonPath("$.data[0].options[0].percentage").value(100.0));
    }
}
