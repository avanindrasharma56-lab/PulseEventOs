package com.eventflow.service.impl;

import com.eventflow.dto.CreateVenueRequest;
import com.eventflow.dto.VenueResponse;
import com.eventflow.entity.Venue;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.VenueRepository;
import com.eventflow.service.VenueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;

    public VenueServiceImpl(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
        return mapToResponse(venue);
    }

    @Override
    public VenueResponse createVenue(CreateVenueRequest request) {
        Venue venue = new Venue(
                request.getName(),
                request.getAddress(),
                request.getCity(),
                request.getState(),
                request.getPostalCode(),
                request.getCountry(),
                request.getCapacity(),
                request.isOnline(),
                request.getStreamUrl()
        );
        Venue saved = venueRepository.save(venue);
        return mapToResponse(saved);
    }

    private VenueResponse mapToResponse(Venue venue) {
        if (venue == null) return null;
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getState(),
                venue.getPostalCode(),
                venue.getCountry(),
                venue.getCapacity(),
                venue.isOnline(),
                venue.getStreamUrl()
        );
    }
}
