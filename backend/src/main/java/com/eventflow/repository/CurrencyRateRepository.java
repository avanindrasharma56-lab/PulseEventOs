package com.eventflow.repository;

import com.eventflow.entity.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
    Optional<CurrencyRate> findByCurrencyCodeIgnoreCase(String currencyCode);
    List<CurrencyRate> findByActiveTrue();
}
