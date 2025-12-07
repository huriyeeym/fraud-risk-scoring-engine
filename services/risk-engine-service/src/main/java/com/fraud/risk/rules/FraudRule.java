package com.fraud.risk.rules;

import com.fraud.risk.model.CustomerProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================
 * FRAUD RULE INTERFACE
 * ============================================
 * Ne yapar?
 * - Tüm fraud kuralları bu interface'i implement eder
 * - Her kural bağımsız çalışır (Single Responsibility)
 * - Her kural bir skor döner (0-100)
 * - Her kural bir açıklama döner (neden tetiklendi?)
 *
 * Neden Interface?
 * - Yeni kural eklemek kolay (Open/Closed Principle)
 * - Test edilebilir (mock edilebilir)
 * - Esnek mimari
 *
 * Örnek Kurallar:
 * - HighAmountRule: Tutar yüksekse
 * - VelocityRule: Çok hızlı işlem varsa
 * - LocationAnomalyRule: Farklı şehirden işlemse
 * - MerchantRiskRule: Riskli merchant kategorisiyse
 */
public interface FraudRule {

    /**
     * ============================================
     * EVALUATE RULE
     * ============================================
     * Ne yapar?
     * - Transaction'ı ve customer profile'ı alır
     * - Kural tetiklendiyse skor döner
     * - Kural tetiklenmediyse 0 döner
     *
     * @param transactionId Transaction ID
     * @param customerId Customer ID
     * @param amount Transaction tutarı
     * @param merchantCategory Merchant kategorisi
     * @param location Lokasyon
     * @param timestamp İşlem zamanı
     * @param customerProfile Müşteri profili (nullable)
     * @return Risk skoru (0-100)
     */
    int evaluate(
            String transactionId,
            String customerId,
            BigDecimal amount,
            String merchantCategory,
            String location,
            LocalDateTime timestamp,
            CustomerProfile customerProfile
    );

    /**
     * ============================================
     * GET RULE NAME
     * ============================================
     * @return Kural adı (örn: "high_amount")
     */
    String getRuleName();

    /**
     * ============================================
     * GET REASON
     * ============================================
     * @return Kural tetiklenme sebebi (örn: "Amount is 3x customer average")
     */
    String getReason();

    /**
     * ============================================
     * GET PRIORITY
     * ============================================
     * Ne yapar?
     * - Kural önceliği (1-10)
     * - Yüksek öncelik = önce çalışır
     *
     * Neden?
     * - Bazı kurallar daha önemli (önce kontrol edilmeli)
     * - Örnek: Velocity rule > Merchant risk rule
     *
     * @return Öncelik (1=düşük, 10=yüksek)
     */
    default int getPriority() {
        return 5;  // Varsayılan orta öncelik
    }
}
