package com.eventflow.dto;

import jakarta.validation.constraints.NotBlank;

public class UpsertNetworkingProfileRequest {
    @NotBlank(message = "Professional headline is required")
    private String headline;
    private String company;
    private String jobTitle;
    private String interests;
    private String networkingGoals;
    private Boolean isAvailable;

    public UpsertNetworkingProfileRequest() {
    }

    public UpsertNetworkingProfileRequest(String headline, String company, String jobTitle,
                                         String interests, String networkingGoals, Boolean isAvailable) {
        this.headline = headline;
        this.company = company;
        this.jobTitle = jobTitle;
        this.interests = interests;
        this.networkingGoals = networkingGoals;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getNetworkingGoals() {
        return networkingGoals;
    }

    public void setNetworkingGoals(String networkingGoals) {
        this.networkingGoals = networkingGoals;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
