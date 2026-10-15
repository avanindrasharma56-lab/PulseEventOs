package com.eventflow.service;

import com.eventflow.dto.CertificateClaimRequest;
import com.eventflow.dto.CertificateDto;
import com.eventflow.dto.CertificateVerificationDto;
import java.util.List;

public interface CertificateService {

    CertificateDto claimCertificate(Long eventId, String username, CertificateClaimRequest request);

    CertificateDto issueCertificateForUser(Long eventId, Long targetUserId, CertificateClaimRequest request);

    byte[] generateCertificatePdf(String certificateCode);

    CertificateVerificationDto verifyCertificate(String certificateCode);

    List<CertificateDto> getMyCertificates(String username);

    List<CertificateDto> getEventCertificates(Long eventId, String requesterUsername);
}
