package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateSponsorRequest;
import com.eventflow.dto.LeadRetrievalDto;
import com.eventflow.dto.LeadScanRequest;
import com.eventflow.dto.SponsorDto;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.SponsorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sponsors & Exhibitor Leads", description = "Sponsor booth directory and lead retrieval engine")
public class SponsorController {

    private final SponsorService sponsorService;

    public SponsorController(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    @GetMapping("/events/{eventId}/sponsors")
    @Operation(summary = "Get all sponsors and exhibitor booths for an event")
    public ResponseEntity<ApiResponse<List<SponsorDto>>> getEventSponsors(@PathVariable Long eventId) {
        List<SponsorDto> sponsors = sponsorService.getSponsorsForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(sponsors));
    }

    @PostMapping("/events/{eventId}/sponsors")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Register a new event sponsor or booth partner")
    public ResponseEntity<ApiResponse<SponsorDto>> createSponsor(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateSponsorRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        SponsorDto created = sponsorService.createSponsor(eventId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Sponsor successfully registered", created));
    }

    @DeleteMapping("/events/{eventId}/sponsors/{sponsorId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Remove a sponsor from an event")
    public ResponseEntity<ApiResponse<Void>> deleteSponsor(
            @PathVariable Long eventId,
            @PathVariable Long sponsorId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        sponsorService.deleteSponsor(eventId, sponsorId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Sponsor deleted", null));
    }

    @PostMapping("/sponsors/{sponsorId}/leads/scan")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Scan an attendee badge QR code to retrieve and qualify an exhibitor lead")
    public ResponseEntity<ApiResponse<LeadRetrievalDto>> scanLead(
            @PathVariable Long sponsorId,
            @Valid @RequestBody LeadScanRequest request,
            @CurrentUser CustomUserDetails currentUser
    ) {
        LeadRetrievalDto lead = sponsorService.scanAttendeeLead(sponsorId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lead captured successfully!", lead));
    }

    @GetMapping("/sponsors/{sponsorId}/leads")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Retrieve all captured leads for a sponsor booth")
    public ResponseEntity<ApiResponse<List<LeadRetrievalDto>>> getSponsorLeads(
            @PathVariable Long sponsorId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        List<LeadRetrievalDto> leads = sponsorService.getSponsorLeads(sponsorId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/sponsors/{sponsorId}/leads/export.csv")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Export exhibitor leads as standard RFC 4180 CSV spreadsheet")
    public ResponseEntity<byte[]> exportLeadsCsv(
            @PathVariable Long sponsorId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        byte[] csvBytes = sponsorService.exportSponsorLeadsCsv(sponsorId, currentUser.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "sponsor-" + sponsorId + "-leads.csv");
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}
