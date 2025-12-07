package com.fraud.risk.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================
 * RISK SCORE MODEL
 * ============================================
 * Ne yapar?
 * - Her transaction için hesaplanan risk skorunu tutar
 * - Rule score + ML score + Final score
 * - Hangi kuralların tetiklendiğini JSONB'de saklar
 *
 * Örnek reasons:
 * {
 *   "high_amount": 30,
 *   "velocity": 40,
 *   "location_anomaly": 25
 * }
 */
@Entity
@Table(name = "risk_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    // ============================================
    // SCORES
    // ============================================
    // Rule score: 0-100 arası (rule engine'den)
    @Column(name = "rule_score")
    private Integer ruleScore;

    // ML score: 0-1 arası (ML model'den fraud probability)
    @Column(name = "ml_score", precision = 5, scale = 2)
    private BigDecimal mlScore;

    // Final score: (rule_score * 0.6) + (ml_score * 40)
    // Örnek: rule=80, ml=0.9 → (80*0.6) + (0.9*40) = 48 + 36 = 84
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    // ============================================
    // REASONS (JSONB)
    // ============================================
    // Hangi kurallar tetiklendi ve ne kadar skor ekledi?
    // PostgreSQL JSONB kolonu
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons", columnDefinition = "jsonb")
    private Map<String, Object> reasons = new HashMap<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    /**
     * Reason ekle
     * Örnek: addReason("high_amount", 30)
     */
    public void addReason(String rule, Object value) {
        this.reasons.put(rule, value);
    }

    /**
     * Final score hesapla
     * Formula: (rule_score * 0.6) + (ml_score * 40)
     */
    public void calculateFinalScore() {
        if (ruleScore != null && mlScore != null) {
            double rulePart = ruleScore * 0.6;
            double mlPart = mlScore.doubleValue() * 40;
            this.finalScore = BigDecimal.valueOf(rulePart + mlPart);
        }
    }
}
