package com.eventflow.controller;

import com.eventflow.dto.AiCopyRequest;
import com.eventflow.dto.LoginRequest;
import com.eventflow.entity.Role;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.repository.RoleRepository;
import com.eventflow.repository.UserRepository;
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

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase13IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User organizer;
    private String organizerToken;
    private User attendee;
    private String attendeeToken;

    @BeforeEach
    void setUp() throws Exception {
        Role organizerRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();

        String uid = UUID.randomUUID().toString().substring(0, 6);

        organizer = new User("org_" + uid + "@phase13.com", passwordEncoder.encode("Password@123"), "Organizer Thirteen", "+1555" + uid);
        organizer.setStatus(UserStatus.ACTIVE);
        organizer.setRoles(Collections.singleton(organizerRole));
        organizer = userRepository.save(organizer);

        attendee = new User("att_" + uid + "@phase13.com", passwordEncoder.encode("Password@123"), "Attendee Thirteen", "+1556" + uid);
        attendee.setStatus(UserStatus.ACTIVE);
        attendee.setRoles(Collections.singleton(attendeeRole));
        attendee = userRepository.save(attendee);

        organizerToken = loginAndGetToken("org_" + uid + "@phase13.com", "Password@123");
        attendeeToken = loginAndGetToken("att_" + uid + "@phase13.com", "Password@123");
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
    @DisplayName("Phase 13: AI Marketing Copywriter generates multi-channel conference assets")
    void testAiCopywriterGeneration() throws Exception {
        AiCopyRequest req = new AiCopyRequest(
                "Kubernetes & Cloud Native AI Architecture",
                "Cloud Computing",
                "Site Reliability Engineers & Platform Architects",
                "Visionary",
                "Zero-downtime microservices, GPU slicing, Multimodal model deployments"
        );

        // 1. Organizer requests AI generation
        MvcResult result = mockMvc.perform(post("/api/ai/generate-event-copy")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titleSuggestions").isArray())
                .andExpect(jsonPath("$.data.recommendedSlug").isNotEmpty())
                .andExpect(jsonPath("$.data.shortSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.fullDescription").isNotEmpty())
                .andExpect(jsonPath("$.data.emailMarketingCopy").isNotEmpty())
                .andExpect(jsonPath("$.data.socialMediaPost").isNotEmpty())
                .andExpect(jsonPath("$.data.suggestedHashtags").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("titleSuggestions").size()).isGreaterThanOrEqualTo(3);
        assertThat(data.path("fullDescription").asText()).contains("Kubernetes & Cloud Native AI Architecture");
        assertThat(data.path("emailMarketingCopy").asText()).contains("Subject:");
        assertThat(data.path("socialMediaPost").asText()).contains("#PulseEventOS");

        // 2. Regular attendee must be rejected (403 Forbidden)
        mockMvc.perform(post("/api/ai/generate-event-copy")
                        .header("Authorization", "Bearer " + attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
