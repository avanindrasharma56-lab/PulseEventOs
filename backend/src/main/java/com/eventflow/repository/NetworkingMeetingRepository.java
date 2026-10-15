package com.eventflow.repository;

import com.eventflow.entity.MeetingStatus;
import com.eventflow.entity.NetworkingMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NetworkingMeetingRepository extends JpaRepository<NetworkingMeeting, Long> {

    @Query("SELECT m FROM NetworkingMeeting m WHERE m.event.id = :eventId " +
           "AND (m.requester.id = :userId OR m.recipient.id = :userId) " +
           "ORDER BY m.startTime ASC")
    List<NetworkingMeeting> findUserMeetingsForEvent(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT m FROM NetworkingMeeting m WHERE m.event.id = :eventId " +
           "AND (m.requester.id = :userId OR m.recipient.id = :userId) " +
           "AND m.status IN (:activeStatuses) " +
           "AND m.startTime < :endTime AND m.endTime > :startTime")
    List<NetworkingMeeting> findConflictingUserMeetings(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<MeetingStatus> activeStatuses
    );

    @Query("SELECT m.tableNumber FROM NetworkingMeeting m WHERE m.event.id = :eventId " +
           "AND m.status IN (:activeStatuses) " +
           "AND m.startTime < :endTime AND m.endTime > :startTime " +
           "AND m.tableNumber IS NOT NULL")
    List<Integer> findOccupiedTables(
            @Param("eventId") Long eventId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<MeetingStatus> activeStatuses
    );
}
