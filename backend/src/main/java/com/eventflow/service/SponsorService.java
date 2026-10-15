package com.eventflow.service;

import com.eventflow.dto.CreateSponsorRequest;
import com.eventflow.dto.LeadRetrievalDto;
import com.eventflow.dto.LeadScanRequest;
import com.eventflow.dto.SponsorDto;

import java.util.List;

public interface SponsorService {
    List<SponsorDto> getSponsorsForEvent(Long eventId);
    SponsorDto createSponsor(Long eventId, CreateSponsorRequest request, Long currentUserId);
    void deleteSponsor(Long eventId, Long sponsorId, Long currentUserId);
    LeadRetrievalDto scanAttendeeLead(Long sponsorId, LeadScanRequest request, Long currentUserId);
    List<LeadRetrievalDto> getSponsorLeads(Long sponsorId, Long currentUserId);
    byte[] exportSponsorLeadsCsv(Long sponsorId, Long currentUserId);
}
