package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CertificateClaimRequest;
import com.eventflow.dto.CertificateDto;
import com.eventflow.dto.CertificateVerificationDto;
import com.eventflow.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Phase 14: Certificates of Attendance & Completion", description = "Endpoints for claiming, issuing, verifying, and generating PDF certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/events/{eventId}/certificates/claim")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Claim Certificate of Attendance/Completion for an event")
    public ResponseEntity<ApiResponse<CertificateDto>> claimCertificate(
            @PathVariable Long eventId,
            @RequestBody(required = false) CertificateClaimRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CertificateDto cert = certificateService.claimCertificate(eventId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Certificate claimed successfully", cert));
    }

    @PostMapping("/events/{eventId}/certificates/issue/{userId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Organizer or Admin issue certificate for an attendee")
    public ResponseEntity<ApiResponse<CertificateDto>> issueCertificate(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestBody(required = false) CertificateClaimRequest request) {
        CertificateDto cert = certificateService.issueCertificateForUser(eventId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Certificate issued successfully", cert));
    }

    @GetMapping("/certificates/{certificateCode}/pdf")
    @Operation(summary = "Download official landscape Certificate PDF")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable String certificateCode) {
        byte[] pdfBytes = certificateService.generateCertificatePdf(certificateCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificate-" + certificateCode + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/certificates/verify/{certificateCode}")
    @Operation(summary = "Public verification endpoint for certificate authenticity")
    public ResponseEntity<ApiResponse<CertificateVerificationDto>> verifyCertificate(@PathVariable String certificateCode) {
        CertificateVerificationDto verification = certificateService.verifyCertificate(certificateCode);
        return ResponseEntity.ok(ApiResponse.success("Verification completed", verification));
    }

    @GetMapping("/certificates/my-certificates")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve all certificates claimed by currently authenticated user")
    public ResponseEntity<ApiResponse<List<CertificateDto>>> getMyCertificates(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CertificateDto> certificates = certificateService.getMyCertificates(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Certificates retrieved successfully", certificates));
    }

    @GetMapping("/events/{eventId}/certificates")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Retrieve all certificates issued for an event")
    public ResponseEntity<ApiResponse<List<CertificateDto>>> getEventCertificates(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CertificateDto> certificates = certificateService.getEventCertificates(eventId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Event certificates retrieved successfully", certificates));
    }
}
