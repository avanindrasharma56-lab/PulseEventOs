package com.eventflow.controller;

import com.eventflow.dto.CreateEventRequest;
import com.eventflow.dto.LoginRequest;
import com.eventflow.dto.RegisterRequest;
import com.eventflow.dto.RejectEventRequest;
import com.eventflow.entity.EventCategory;
import com.eventflow.entity.Role;
import com.eventflow.entity.User;
import com.eventflow.entity.Venue;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.repository.EventCategoryRepository;
import com.eventflow.repository.RoleRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.repository.VenueRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventLifecycleIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private String organizerToken;
    private String adminToken;
    private Long organizerId;
    private Long adminId;
    private Long categoryId;
    private Long venueId;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        // Create Organizer
        User organizer = new User("dave.org@example.com", passwordEncoder.encode("OrgPass!123"), "Dave Organizer", null);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);
        organizerId = organizer.getId();

        // Create Admin
        User admin = new User("sarah.admin@example.com", passwordEncoder.encode("AdminPass!123"), "Sarah Admin", null);
        admin.setRoles(Collections.singleton(adminRole));
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);
        adminId = admin.getId();

        // Login Organizer
        LoginRequest orgLogin = new LoginRequest("dave.org@example.com", "OrgPass!123");
        MvcResult orgRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgLogin)))
                .andExpect(status().isOk())
                .andReturn();
        organizerToken = extractToken(orgRes);

        // Login Admin
        LoginRequest adminLogin = new LoginRequest("sarah.admin@example.com", "AdminPass!123");
        MvcResult adminRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = extractToken(adminRes);

        // Fetch or create Category & Venue
        EventCategory category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            EventCategory c = new EventCategory("AI Innovation", "ai-innovation", "AI tech", "Cpu");
            return categoryRepository.save(c);
        });
        categoryId = category.getId();

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue("Civic Center", "100 Main St", "San Francisco", "CA", "94102", "USA", 1000, false, null);
            return venueRepository.save(v);
        });
        venueId = venue.getId();
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete Event Lifecycle: Draft -> Submit -> Self-Approval Block -> Reject -> Resubmit -> Approve -> Publish -> Public Discovery")
    void testCompleteEventWorkflow() throws Exception {
        // 1. Organizer creates draft event
        CreateEventRequest createReq = new CreateEventRequest();
        createReq.setTitle("Global AI Summit 2026");
        createReq.setCategoryId(categoryId);
        createReq.setVenueId(venueId);
        createReq.setShortSummary("The premiere artificial intelligence conference");
        createReq.setDescription("Explore breakthroughs in foundation models, autonomous agents, and AI hardware.");
        createReq.setBannerImageUrl("https://images.unsplash.com/photo-1540575467063-178a50c2df87");
        createReq.setStartTime(LocalDateTime.now().plusDays(10));
        createReq.setEndTime(LocalDateTime.now().plusDays(12));
        createReq.setFeatured(true);

        MvcResult createRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.slug").value("global-ai-summit-2026"))
                .andReturn();

        Long eventId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Organizer submits event for approval
        mockMvc.perform(post("/api/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // 3. Admin queue lists the pending event
        mockMvc.perform(get("/api/admin/events/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(eventId));

        // 4. Admin rejects with a reason
        RejectEventRequest rejectReq = new RejectEventRequest("Please specify the keynote speaker lineup in the description");
        mockMvc.perform(post("/api/admin/events/" + eventId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Please specify the keynote speaker lineup in the description"));

        // 5. Organizer resubmits event
        mockMvc.perform(post("/api/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // 6. Admin approves event
        mockMvc.perform(post("/api/admin/events/" + eventId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 7. Organizer publishes the approved event
        mockMvc.perform(post("/api/events/" + eventId + "/publish")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 8. Public attendee (unauthenticated) searches published events
        mockMvc.perform(get("/api/events")
                        .param("search", "Summit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Global AI Summit 2026"));

        // 9. Public attendee gets event details by slug
        mockMvc.perform(get("/api/events/global-ai-summit-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("global-ai-summit-2026"))
                .andExpect(jsonPath("$.data.organizerName").value("Dave Organizer"));
    }

    @Test
    @DisplayName("Self-approval rule: Admin cannot approve their own organized event")
    void testSelfApprovalProhibited() throws Exception {
        // Create an Admin who is also an organizer
        CreateEventRequest createReq = new CreateEventRequest();
        createReq.setTitle("Admin Organised Conference");
        createReq.setCategoryId(categoryId);
        createReq.setVenueId(venueId);
        createReq.setShortSummary("Admin self event");
        createReq.setStartTime(LocalDateTime.now().plusDays(5));
        createReq.setEndTime(LocalDateTime.now().plusDays(6));

        MvcResult createRes = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Submit for approval
        mockMvc.perform(post("/api/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // Admin attempts to approve their own event -> must be rejected with 400 Bad Request
        mockMvc.perform(post("/api/admin/events/" + eventId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Self-approval prohibited: An organizer cannot approve their own event"));
    }
}
