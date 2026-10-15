package com.eventflow.dto;

import java.util.ArrayList;
import java.util.List;

public class RoomHeatmapDto {

    private String room;
    private String track;
    private Integer sessionCount;
    private Integer totalCapacity;
    private Integer totalReserved;
    private Double averageOccupancyPercentage;
    private List<SessionCapacityDto> sessions = new ArrayList<>();

    public RoomHeatmapDto() {
    }

    public RoomHeatmapDto(String room, String track) {
        this.room = room;
        this.track = track;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public Integer getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getTotalReserved() {
        return totalReserved;
    }

    public void setTotalReserved(Integer totalReserved) {
        this.totalReserved = totalReserved;
    }

    public Double getAverageOccupancyPercentage() {
        return averageOccupancyPercentage;
    }

    public void setAverageOccupancyPercentage(Double averageOccupancyPercentage) {
        this.averageOccupancyPercentage = averageOccupancyPercentage;
    }

    public List<SessionCapacityDto> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionCapacityDto> sessions) {
        this.sessions = sessions;
    }
}
