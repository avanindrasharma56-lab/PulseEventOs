package com.eventflow.dto;

import com.eventflow.entity.enums.SessionType;
import java.time.LocalDateTime;

public class EventSessionDto {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private SpeakerDto speaker;
    private String title;
    private String description;
    private String room;
    private String track;
    private SessionType sessionType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isBookmarked = false;
    private Integer maxCapacity = 100;
    private Integer reservedSeats = 0;
    private Integer availableSeats = 100;
    private boolean isRsvpd = false;
    private String rsvpStatus;
    private String rsvpCode;

    public EventSessionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public SpeakerDto getSpeaker() { return speaker; }
    public void setSpeaker(SpeakerDto speaker) { this.speaker = speaker; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }

    public SessionType getSessionType() { return sessionType; }
    public void setSessionType(SessionType sessionType) { this.sessionType = sessionType; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public Integer getReservedSeats() { return reservedSeats; }
    public void setReservedSeats(Integer reservedSeats) { this.reservedSeats = reservedSeats; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public boolean isRsvpd() { return isRsvpd; }
    public void setRsvpd(boolean rsvpd) { isRsvpd = rsvpd; }

    public String getRsvpStatus() { return rsvpStatus; }
    public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }

    public String getRsvpCode() { return rsvpCode; }
    public void setRsvpCode(String rsvpCode) { this.rsvpCode = rsvpCode; }
}
