package com.eventflow.service.impl;

import com.eventflow.dto.AiCopyRequest;
import com.eventflow.dto.AiCopyResponse;
import com.eventflow.service.AiCopywriterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiCopywriterServiceImpl implements AiCopywriterService {

    private static final Logger log = LoggerFactory.getLogger(AiCopywriterServiceImpl.class);

    @Value("${gemini.api.key:#{null}}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AiCopywriterServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AiCopyResponse generateEventCopy(AiCopyRequest request, Long currentUserId) {
        String apiKey = geminiApiKey != null && !geminiApiKey.isBlank() ? geminiApiKey : System.getenv("GEMINI_API_KEY");

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return callGeminiApi(request, apiKey);
            } catch (Exception ex) {
                log.warn("Gemini API call failed, falling back to local template synthesizer: {}", ex.getMessage());
            }
        }

        return synthesizeCopywriting(request);
    }

    private AiCopyResponse callGeminiApi(AiCopyRequest req, String apiKey) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String prompt = "You are a world-class technology conference marketing copywriter. " +
                "Generate compelling event marketing assets in JSON for the following event idea:\n" +
                "Topic: " + req.getTopic() + "\n" +
                "Category: " + req.getCategory() + "\n" +
                "Target Audience: " + req.getTargetAudience() + "\n" +
                "Tone: " + req.getTone() + "\n" +
                "Key Highlights: " + req.getKeyHighlights() + "\n\n" +
                "Respond ONLY with a valid JSON object matching this schema:\n" +
                "{\n" +
                "  \"titleSuggestions\": [\"Title 1\", \"Title 2\", \"Title 3\"],\n" +
                "  \"recommendedSlug\": \"slug-lowercase\",\n" +
                "  \"shortSummary\": \"Punchy 1-2 sentence overview\",\n" +
                "  \"fullDescription\": \"Multi-paragraph detailed conference description with takeaways\",\n" +
                "  \"emailMarketingCopy\": \"Subject: ... Body: ... Call to Action: ...\",\n" +
                "  \"socialMediaPost\": \"Engaging social post for LinkedIn and X with emojis\",\n" +
                "  \"suggestedHashtags\": [\"#Tag1\", \"#Tag2\", \"#Tag3\"]\n" +
                "}";

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String rawText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Extract JSON substring if wrapped in markdown code blocks
            String jsonStr = rawText;
            if (rawText.contains("```json")) {
                jsonStr = rawText.substring(rawText.indexOf("```json") + 7);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            } else if (rawText.contains("```")) {
                jsonStr = rawText.substring(rawText.indexOf("```") + 3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            }

            return objectMapper.readValue(jsonStr.trim(), AiCopyResponse.class);
        }

        return synthesizeCopywriting(req);
    }

    private AiCopyResponse synthesizeCopywriting(AiCopyRequest req) {
        String topic = req.getTopic().trim();
        String cat = req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory() : "Technology";
        String audience = req.getTargetAudience() != null && !req.getTargetAudience().isBlank() ? req.getTargetAudience() : "Industry Leaders & Practitioners";
        String tone = req.getTone() != null ? req.getTone() : "Professional";
        String highlights = req.getKeyHighlights() != null && !req.getKeyHighlights().isBlank() ? req.getKeyHighlights() : "Interactive Keynotes, Architecture Deep Dives, Hands-on Workshops";

        String cleanSlug = topic.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") + "-2026";

        List<String> titles = Arrays.asList(
                topic + " Summit 2026: The Premier " + cat + " Experience",
                "Next-Gen " + topic + ": Unlocking Innovation for " + audience,
                "Accelerate with " + topic + ": Visionary Keynotes & Architecture Masterclass"
        );

        String summary = "The definitive " + tone.toLowerCase() + " gathering for " + audience +
                ", exploring modern breakthroughs, production strategies, and hands-on paradigms in " + topic + ".";

        String description = "Welcome to the " + topic + " Conference!\n\n" +
                "Designed specifically for " + audience + ", this gathering brings together innovators, practitioners, and executives " +
                "for a multi-day journey into the latest evolution of " + cat + ".\n\n" +
                "### Key Program Highlights:\n" +
                "• " + highlights.replace(", ", "\n• ") + "\n" +
                "• In-depth technical architecture breakdowns and zero-downtime blueprints.\n" +
                "• Live roundtables, keynote panels, and VIP networking lounges.\n\n" +
                "### Who Should Attend:\n" +
                "Engineers, directors, system architects, and professionals looking to master " + topic + " and drive high-impact transformation.\n\n" +
                "Reserve your digital admission pass today to secure early-bird access and reserved venue seating.";

        String email = "Subject: 🚀 You're Invited: " + topic + " Summit 2026\n\n" +
                "Hi there,\n\n" +
                "We are thrilled to officially announce the " + topic + " Conference — an exclusive summit built for " + audience + ".\n\n" +
                "Whether you're looking to scale production workloads, discover breakthrough tooling, or connect with world-class peers, this is the must-attend event of the year.\n\n" +
                "Highlights include:\n" +
                "• " + highlights + "\n\n" +
                "Digital passes and early-bird seats are strictly limited. View the agenda and reserve your pass now: https://eventflow.io/events/" + cleanSlug + "\n\n" +
                "Best regards,\n" +
                "The PulseEvent Organizing Committee";

        String social = "🔥 Excited to unveil " + topic + " Summit 2026!\n\n" +
                "Calling all " + audience + ": Join us for deep dives into " + cat + " breakthroughs, live architectures, and exclusive networking.\n\n" +
                "✨ Highlights: " + highlights + "\n\n" +
                "🎟️ Passes now open! Grab yours before the early-bird tier sells out 👉 https://eventflow.io/events/" + cleanSlug + "\n\n" +
                "#" + topic.replaceAll("[^a-zA-Z0-9]", "") + " #" + cat.replaceAll("[^a-zA-Z0-9]", "") + " #PulseEventOS #TechConference";

        List<String> hashtags = Arrays.asList(
                "#" + topic.replaceAll("[^a-zA-Z0-9]", ""),
                "#" + cat.replaceAll("[^a-zA-Z0-9]", ""),
                "#PulseEventOS",
                "#Conference2026"
        );

        return new AiCopyResponse(titles, cleanSlug, summary, description, email, social, hashtags);
    }
}
