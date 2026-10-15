package com.eventflow.dto;

public class PollOptionDto {

    private Long id;
    private String optionText;
    private Integer voteCount;
    private Double percentage;
    private Integer displayOrder;

    public PollOptionDto() {
    }

    public PollOptionDto(Long id, String optionText, Integer voteCount, Double percentage, Integer displayOrder) {
        this.id = id;
        this.optionText = optionText;
        this.voteCount = voteCount;
        this.percentage = percentage;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
