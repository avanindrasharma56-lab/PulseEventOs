package com.eventflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "poll_options")
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore
    private LivePoll poll;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public PollOption() {
    }

    public PollOption(LivePoll poll, String optionText, Integer displayOrder) {
        this.poll = poll;
        this.optionText = optionText;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.voteCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LivePoll getPoll() {
        return poll;
    }

    public void setPoll(LivePoll poll) {
        this.poll = poll;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
