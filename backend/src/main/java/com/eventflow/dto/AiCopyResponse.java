package com.eventflow.dto;

import java.util.List;

public class AiCopyResponse {
    private List<String> titleSuggestions;
    private String recommendedSlug;
    private String shortSummary;
    private String fullDescription;
    private String emailMarketingCopy;
    private String socialMediaPost;
    private List<String> suggestedHashtags;

    public AiCopyResponse() {}

    public AiCopyResponse(List<String> titleSuggestions, String recommendedSlug, String shortSummary, String fullDescription, String emailMarketingCopy, String socialMediaPost, List<String> suggestedHashtags) {
        this.titleSuggestions = titleSuggestions;
        this.recommendedSlug = recommendedSlug;
        this.shortSummary = shortSummary;
        this.fullDescription = fullDescription;
        this.emailMarketingCopy = emailMarketingCopy;
        this.socialMediaPost = socialMediaPost;
        this.suggestedHashtags = suggestedHashtags;
    }

    public List<String> getTitleSuggestions() { return titleSuggestions; }
    public void setTitleSuggestions(List<String> titleSuggestions) { this.titleSuggestions = titleSuggestions; }

    public String getRecommendedSlug() { return recommendedSlug; }
    public void setRecommendedSlug(String recommendedSlug) { this.recommendedSlug = recommendedSlug; }

    public String getShortSummary() { return shortSummary; }
    public void setShortSummary(String shortSummary) { this.shortSummary = shortSummary; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public String getEmailMarketingCopy() { return emailMarketingCopy; }
    public void setEmailMarketingCopy(String emailMarketingCopy) { this.emailMarketingCopy = emailMarketingCopy; }

    public String getSocialMediaPost() { return socialMediaPost; }
    public void setSocialMediaPost(String socialMediaPost) { this.socialMediaPost = socialMediaPost; }

    public List<String> getSuggestedHashtags() { return suggestedHashtags; }
    public void setSuggestedHashtags(List<String> suggestedHashtags) { this.suggestedHashtags = suggestedHashtags; }
}
