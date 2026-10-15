package com.eventflow.repository;

import com.eventflow.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateCode(String certificateCode);

    Optional<Certificate> findByEventIdAndUserId(Long eventId, Long userId);

    List<Certificate> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Certificate> findByEventIdOrderByCreatedAtDesc(Long eventId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
