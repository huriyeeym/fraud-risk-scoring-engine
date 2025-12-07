package com.fraud.risk.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ============================================
 * CUSTOMER PROFILE MODEL
 * ============================================
 * Ne yapar?
 * - Müşterinin davranış profilini tutar
 * - Risk Engine bu profili kullanarak anomali tespiti yapar
 * - Redis'te cache'lenir (hızlı erişim)
 *
 * Nasıl oluşturulur?
 * - Batch job her gün çalışır, son 30 günün verilerini analiz eder
 * - avg_amount, frequent_locations, vb. hesaplanır
 *
 * Neden Serializable?
 * - Redis cache için gerekli
 */
@Entity
@Table(name = "customer_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "customer_id", length = 50)
    private String customerId;

    // ============================================
    // STATISTICAL INFORMATION
    // ============================================
    // Ortalama işlem tutarı
    @Column(name = "avg_amount", precision = 10, scale = 2)
    private BigDecimal avgAmount;

    // Medyan tutar
    @Column(name = "median_amount", precision = 10, scale = 2)
    private BigDecimal medianAmount;

    // Standart sapma (ne kadar değişken?)
    @Column(name = "std_amount", precision = 10, scale = 2)
    private BigDecimal stdAmount;

    // ============================================
    // LOCATION DATA (JSONB)
    // ============================================
    // Sık kullanılan lokasyonlar
    // Örnek: ["Istanbul", "Ankara", "Izmir"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frequent_locations", columnDefinition = "jsonb")
    private List<String> frequentLocations;

    // ============================================
    // MERCHANT CATEGORY DATA (JSONB)
    // ============================================
    // Merchant kategori dağılımı
    // Örnek: {"electronics": 0.4, "food": 0.5, "clothing": 0.1}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "merchant_categories", columnDefinition = "jsonb")
    private Map<String, Double> merchantCategories;

    // ============================================
    // TIME DISTRIBUTION (JSONB)
    // ============================================
    // Zaman bazlı davranış
    // Örnek: {"morning": 0.2, "afternoon": 0.5, "evening": 0.3}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "time_distribution", columnDefinition = "jsonb")
    private Map<String, Double> timeDistribution;

    // ============================================
    // TRANSACTION COUNTS
    // ============================================
    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "first_transaction_date")
    private LocalDate firstTransactionDate;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // ============================================
    // HELPER METHODS
    // ============================================
    /**
     * Lokasyon profilde var mı?
     */
    public boolean isLocationFrequent(String location) {
        return frequentLocations != null && frequentLocations.contains(location);
    }

    /**
     * Tutar normal aralıkta mı?
     * Normal: avg ± (3 * std)
     */
    public boolean isAmountNormal(BigDecimal amount) {
        if (avgAmount == null || stdAmount == null) {
            return true;  // Profile yoksa normal kabul et
        }

        BigDecimal lowerBound = avgAmount.subtract(stdAmount.multiply(BigDecimal.valueOf(3)));
        BigDecimal upperBound = avgAmount.add(stdAmount.multiply(BigDecimal.valueOf(3)));

        return amount.compareTo(lowerBound) >= 0 && amount.compareTo(upperBound) <= 0;
    }

    /**
     * Tutar ortalamadan kaç kat fazla?
     */
    public double getAmountMultiplier(BigDecimal amount) {
        if (avgAmount == null || avgAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 1.0;
        }
        return amount.divide(avgAmount, 2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
