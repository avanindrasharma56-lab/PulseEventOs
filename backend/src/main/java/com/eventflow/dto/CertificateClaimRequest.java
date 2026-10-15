package com.eventflow.dto;

import com.eventflow.entity.enums.CertificateType;

public class CertificateClaimRequest {

    private CertificateType certificateType = CertificateType.COMPLETION;

    public CertificateClaimRequest() {
    }

    public CertificateClaimRequest(CertificateType certificateType) {
        this.certificateType = certificateType;
    }

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }
}
