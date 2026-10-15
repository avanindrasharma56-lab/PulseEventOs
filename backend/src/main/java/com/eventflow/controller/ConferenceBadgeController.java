package com.eventflow.controller;

import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.ConferenceBadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Conference Badges", description = "Printable attendee badges and bulk kiosk lanyard sheets")
public class ConferenceBadgeController {

    private final ConferenceBadgeService conferenceBadgeService;

    public ConferenceBadgeController(ConferenceBadgeService conferenceBadgeService) {
        this.conferenceBadgeService = conferenceBadgeService;
    }

    @GetMapping("/tickets/{ticketCode}/badge.pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download printable 4x6 lanyard badge PDF for individual attendee")
    public ResponseEntity<byte[]> downloadSingleBadgePdf(
            @PathVariable String ticketCode,
            @CurrentUser CustomUserDetails currentUser
    ) {
        byte[] pdfBytes = conferenceBadgeService.generateSingleBadgePdf(ticketCode, currentUser.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "badge-" + ticketCode + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/events/{eventId}/badges.pdf")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Bulk export all attendee badges into unified printable PDF for check-in kiosks")
    public ResponseEntity<byte[]> downloadBulkBadgesPdf(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        byte[] pdfBytes = conferenceBadgeService.generateBulkBadgesPdf(eventId, currentUser.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "event-" + eventId + "-all-badges.pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
