package com.eventflow.service;

import com.eventflow.dto.CreateVenueRequest;
import com.eventflow.dto.VenueResponse;

import java.util.List;

public interface VenueService {
    List<VenueResponse> getAllVenues();
    VenueResponse getVenueById(Long id);
    VenueResponse createVenue(CreateVenueRequest request);
}
