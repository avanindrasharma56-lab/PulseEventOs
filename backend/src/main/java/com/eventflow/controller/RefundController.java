package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateRefundRequest;
import com.eventflow.dto.RefundRequestResponse;
import com.eventflow.dto.ReviewRefundRequest;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@Tag(name = "Refunds & Cancellations", description = "Self-service attendee refunds, administrative approvals, and automated inventory restock")
@SecurityRequirement(name = "Bearer Authentication")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/request")
    @Operation(summary = "Submit an attendee self-service refund request")
    public ResponseEntity<ApiResponse<RefundRequestResponse>> requestRefund(
            @Valid @RequestBody CreateRefundRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        RefundRequestResponse response = refundService.requestRefund(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Refund request submitted successfully", response));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get all refund requests submitted by the current user")
    public ResponseEntity<ApiResponse<List<RefundRequestResponse>>> getMyRefundRequests(
            @CurrentUser CustomUserDetails currentUser) {
        List<RefundRequestResponse> requests = refundService.getMyRefundRequests(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get all refund requests (Organizer or Administrator)")
    public ResponseEntity<ApiResponse<List<RefundRequestResponse>>> getAllRefundRequests(
            @CurrentUser CustomUserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<RefundRequestResponse> list;
        if (isAdmin) {
            list = refundService.getAllRefundRequests();
        } else {
            list = refundService.getOrganizerRefundRequests(currentUser.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Review and process a refund request (Approve or Reject)")
    public ResponseEntity<ApiResponse<RefundRequestResponse>> reviewRefund(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRefundRequest review,
            @CurrentUser CustomUserDetails currentUser) {
        RefundRequestResponse response = refundService.processRefund(currentUser.getId(), id, review);
        String msg = Boolean.TRUE.equals(review.getApproved())
                ? "Refund approved, tickets cancelled, and inventory restocked"
                : "Refund request rejected";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }
}
