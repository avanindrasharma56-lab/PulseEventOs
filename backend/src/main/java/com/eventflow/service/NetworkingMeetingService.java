package com.eventflow.service;

import com.eventflow.dto.NetworkingMeetingDto;
import com.eventflow.dto.NetworkingProfileDto;
import com.eventflow.dto.RequestMeetingRequest;
import com.eventflow.dto.UpsertNetworkingProfileRequest;

import java.util.List;

public interface NetworkingMeetingService {

    NetworkingProfileDto upsertProfile(Long eventId, Long userId, UpsertNetworkingProfileRequest request);

    NetworkingProfileDto getProfile(Long eventId, Long userId);

    List<NetworkingProfileDto> searchProfiles(Long eventId, String query, String interest);

    NetworkingMeetingDto requestMeeting(Long eventId, Long requesterId, RequestMeetingRequest request);

    NetworkingMeetingDto respondMeeting(Long meetingId, Long userId, boolean accept);

    NetworkingMeetingDto cancelMeeting(Long meetingId, Long userId);

    List<NetworkingMeetingDto> getMyMeetings(Long eventId, Long userId);
}
