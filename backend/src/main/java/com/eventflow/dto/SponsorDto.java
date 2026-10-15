package com.eventflow.dto;

import com.eventflow.entity.enums.SponsorTier;

public class SponsorDto {
    private Long id;
    private Long eventId;
    private String name;
    private SponsorTier tier;
    private String logoUrl;
    private String websiteUrl;
    private String boothNumber;
    private String description;
    private Integer displayOrder;

    public SponsorDto() {}

    public SponsorDto(Long id, Long eventId, String name, SponsorTier tier, String logoUrl, String websiteUrl, String boothNumber, String description, Integer displayOrder) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.tier = tier;
        this.logoUrl = logoUrl;
        this.websiteUrl = websiteUrl;
        this.boothNumber = boothNumber;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SponsorTier getTier() { return tier; }
    public void setTier(SponsorTier tier) { this.tier = tier; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getBoothNumber() { return boothNumber; }
    public void setBoothNumber(String boothNumber) { this.boothNumber = boothNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
