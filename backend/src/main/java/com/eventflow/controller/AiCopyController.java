package com.eventflow.controller;

import com.eventflow.dto.AiCopyRequest;
import com.eventflow.dto.AiCopyResponse;
import com.eventflow.dto.ApiResponse;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.AiCopywriterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Marketing & Copywriting", description = "AI-powered conference titles, descriptions, and promotional blasts")
public class AiCopyController {

    private final AiCopywriterService aiCopywriterService;

    public AiCopyController(AiCopywriterService aiCopywriterService) {
        this.aiCopywriterService = aiCopywriterService;
    }

    @PostMapping("/generate-event-copy")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Generate titles, summaries, descriptions, email blasts, and social media copy with Gemini AI")
    public ResponseEntity<ApiResponse<AiCopyResponse>> generateEventCopy(
            @Valid @RequestBody AiCopyRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        AiCopyResponse response = aiCopywriterService.generateEventCopy(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("AI marketing copy generated successfully!", response));
    }
}
