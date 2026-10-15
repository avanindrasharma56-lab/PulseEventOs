package com.eventflow.repository;

import com.eventflow.entity.RefundRequest;
import com.eventflow.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    List<RefundRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<RefundRequest> findByOrderId(Long orderId);

    List<RefundRequest> findByStatusOrderByCreatedAtAsc(RefundStatus status);

    List<RefundRequest> findAllByOrderByCreatedAtDesc();

    List<RefundRequest> findByOrderEventOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<RefundStatus> statuses);
}
