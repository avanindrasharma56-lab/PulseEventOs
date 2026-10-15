package com.eventflow.dto;

import com.eventflow.entity.enums.SponsorTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateSponsorRequest {

    @NotBlank(message = "Sponsor name is required")
    private String name;

    @NotNull(message = "Sponsor tier is required")
    private SponsorTier tier = SponsorTier.GOLD;

    private String logoUrl;
    private String websiteUrl;
    private String boothNumber;
    private String description;
    private Integer displayOrder = 0;

    public CreateSponsorRequest() {}

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
