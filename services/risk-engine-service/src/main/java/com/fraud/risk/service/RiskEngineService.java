package com.fraud.risk.service;

import com.fraud.risk.kafka.AlertProducer;
import com.fraud.risk.model.*;
import com.fraud.risk.repository.CustomerProfileRepository;
import com.fraud.risk.repository.RiskScoreRepository;
import com.fraud.risk.rules.FraudRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================
 * RISK ENGINE SERVICE
 * ============================================
 * CORE FRAUD DETECTION ENGINE
 *
 * İş Akışı:
 * 1. Transaction al (Kafka'dan)
 * 2. Customer profile getir (Redis/PostgreSQL)
 * 3. Tüm fraud kurallarını çalıştır
 * 4. ML servisini çağır (fraud probability)
 * 5. Hybrid skor hesapla: (rule_score * 0.6) + (ml_score * 40)
 * 6. Risk score kaydet (PostgreSQL)
 * 7. Eğer skor > threshold ise Alert gönder (Kafka)
 */
@Service
public class RiskEngineService {

    private static final Logger logger = LoggerFactory.getLogger(RiskEngineService.class);

    private final List<FraudRule> fraudRules;
    private final CustomerProfileRepository customerProfileRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final AlertProducer alertProducer;

    @Value("${fraud.detection.alert-threshold:70}")
    private double alertThreshold;

    public RiskEngineService(
            List<FraudRule> fraudRules,
            CustomerProfileRepository customerProfileRepository,
            RiskScoreRepository riskScoreRepository,
            AlertProducer alertProducer) {
        this.fraudRules = fraudRules;
        this.customerProfileRepository = customerProfileRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.alertProducer = alertProducer;
    }

    /**
     * ============================================
     * ANALYZE TRANSACTION (MAIN LOGIC)
     * ============================================
     */
    @Transactional
    public void analyzeTransaction(Transaction transaction) {
        logger.info("Analyzing transaction: ID={}, Customer={}, Amount={}",
                transaction.getTransactionId(),
                transaction.getCustomerId(),
                transaction.getAmount());

        // ========== 1. GET CUSTOMER PROFILE ==========
        CustomerProfile profile = customerProfileRepository
                .findById(transaction.getCustomerId())
                .orElse(null);

        if (profile == null) {
            logger.warn("Customer profile not found: {}", transaction.getCustomerId());
        }

        // ========== 2. RUN FRAUD RULES ==========
        int totalRuleScore = 0;
        Map<String, Object> reasons = new HashMap<>();

        // Kuralları öncelik sırasına göre çalıştır
        fraudRules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        for (FraudRule rule : fraudRules) {
            try {
                int score = rule.evaluate(
                        transaction.getTransactionId(),
                        transaction.getCustomerId(),
                        transaction.getAmount(),
                        transaction.getMerchantCategory(),
                        transaction.getLocation(),
                        transaction.getTimestamp(),
                        profile
                );

                if (score > 0) {
                    totalRuleScore += score;
                    reasons.put(rule.getRuleName(), rule.getReason());

                    logger.info("Rule triggered: {} - Score: {} - Reason: {}",
                            rule.getRuleName(), score, rule.getReason());
                }
            } catch (Exception e) {
                logger.error("Error executing rule: {} - Error: {}",
                        rule.getRuleName(), e.getMessage(), e);
            }
        }

        // Rule score cap (max 100)
        totalRuleScore = Math.min(totalRuleScore, 100);

        logger.info("Total Rule Score: {}", totalRuleScore);

        // ========== 3. CALL ML SERVICE ==========
        // TODO: Implement ML Service client
        // For now, use placeholder
        BigDecimal mlScore = BigDecimal.valueOf(0.5);  // Placeholder: 0.5 (50% fraud probability)

        logger.info("ML Score (placeholder): {}", mlScore);

        // ========== 4. CALCULATE FINAL SCORE ==========
        RiskScore riskScore = new RiskScore();
        riskScore.setTransactionId(transaction.getTransactionId());
        riskScore.setRuleScore(totalRuleScore);
        riskScore.setMlScore(mlScore);
        riskScore.setReasons(reasons);
        riskScore.calculateFinalScore();  // (rule * 0.6) + (ml * 40)

        logger.info("Final Risk Score: {}", riskScore.getFinalScore());

        // ========== 5. SAVE RISK SCORE ==========
        riskScoreRepository.save(riskScore);

        // ========== 6. SEND ALERT IF HIGH RISK ==========
        if (riskScore.getFinalScore().doubleValue() > alertThreshold) {
            Alert alert = new Alert(
                    transaction.getTransactionId(),
                    transaction.getCustomerId(),
                    transaction.getAmount(),
                    riskScore.getFinalScore(),
                    reasons,
                    LocalDateTime.now()
            );

            alertProducer.sendAlert(alert);

            logger.warn("HIGH RISK ALERT SENT - Transaction: {}, Score: {}",
                    transaction.getTransactionId(), riskScore.getFinalScore());
        }

        logger.info("Transaction analysis completed: ID={}", transaction.getTransactionId());
    }
}
