package com.fraud.risk.repository;

import com.fraud.risk.model.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
    Optional<RiskScore> findByTransactionId(String transactionId);
    List<RiskScore> findByFinalScoreGreaterThan(java.math.BigDecimal threshold);
}
