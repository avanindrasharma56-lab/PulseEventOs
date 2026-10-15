package com.eventflow.repository;

import com.eventflow.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<PromoCode> findAllByOrderByCreatedAtDesc();
}
