package com.eventflow.repository;

import com.eventflow.entity.Ticket;
import com.eventflow.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByOrderUserId(Long userId);

    List<Ticket> findByOrderUserIdAndTicketTierEventId(Long userId, Long eventId);

    List<Ticket> findByOrderId(Long orderId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByTicketTierEventIdOrderByCreatedAtDesc(Long eventId);

    long countByTicketTierEventId(Long eventId);

    long countByTicketTierEventIdAndStatus(Long eventId, TicketStatus status);

    long countByTicketTierIdAndStatus(Long tierId, TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByTicketTierEventIdAndStatusAndCheckedInAtAfter(Long eventId, TicketStatus status, java.time.LocalDateTime after);

    boolean existsByOrderUserIdAndTicketTierEventId(Long userId, Long eventId);
}
