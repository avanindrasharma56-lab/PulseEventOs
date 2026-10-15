package com.eventflow.controller;

import com.eventflow.dto.ApiResponse;
import com.eventflow.dto.CreateVenueRequest;
import com.eventflow.dto.VenueResponse;
import com.eventflow.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> venues = venueService.getAllVenues();
        return ResponseEntity.ok(ApiResponse.success(venues));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Long id) {
        VenueResponse venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(venue));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        VenueResponse venue = venueService.createVenue(request);
        return new ResponseEntity<>(ApiResponse.success("Venue created successfully", venue), HttpStatus.CREATED);
    }
}
