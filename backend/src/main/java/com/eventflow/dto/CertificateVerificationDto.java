package com.eventflow.dto;

import com.eventflow.entity.enums.CertificateType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CertificateVerificationDto {

    private boolean valid;
    private String certificateCode;
    private String recipientName;
    private String eventTitle;
    private LocalDate issueDate;
    private CertificateType certificateType;
    private Integer hoursCompleted;
    private String organizerName;
    private String venueName;
    private LocalDateTime verifiedAt;
    private String message;

    public CertificateVerificationDto() {
    }

    public static CertificateVerificationDto invalid(String code, String message) {
        CertificateVerificationDto dto = new CertificateVerificationDto();
        dto.setValid(false);
        dto.setCertificateCode(code);
        dto.setMessage(message);
        dto.setVerifiedAt(LocalDateTime.now());
        return dto;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }

    public Integer getHoursCompleted() {
        return hoursCompleted;
    }

    public void setHoursCompleted(Integer hoursCompleted) {
        this.hoursCompleted = hoursCompleted;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
