package com.fraud.risk.rules;

import com.fraud.risk.model.CustomerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================
 * HIGH AMOUNT RULE
 * ============================================
 * Ne yapar?
 * - Transaction tutarı müşterinin ortalamasının X katından fazlaysa → şüpheli
 *
 * Örnek:
 * - Müşterinin avg_amount: 500 TL
 * - Multiplier: 3
 * - Transaction: 2000 TL
 * - 2000 > (500 * 3) → Tetiklen! (30 puan)
 *
 * Neden?
 * - Ani büyük harcamalar genelde fraud göstergesi
 * - Çalıntı kart: Hızlıca büyük alışveriş yapılır
 */
@Component
public class HighAmountRule implements FraudRule {

    private static final Logger logger = LoggerFactory.getLogger(HighAmountRule.class);

    // application.yml'den gelir (default: 3)
    @Value("${fraud.detection.high-amount-multiplier:3}")
    private double multiplier;

    private String reason;

    @Override
    public int evaluate(
            String transactionId,
            String customerId,
            BigDecimal amount,
            String merchantCategory,
            String location,
            LocalDateTime timestamp,
            CustomerProfile customerProfile) {

        // Profile yoksa kural çalışmaz
        if (customerProfile == null || customerProfile.getAvgAmount() == null) {
            logger.debug("Customer profile not found or avg_amount is null: {}", customerId);
            return 0;
        }

        BigDecimal avgAmount = customerProfile.getAvgAmount();
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(multiplier));

        logger.debug("High Amount Rule - Customer: {}, Amount: {}, Avg: {}, Threshold: {}",
                customerId, amount, avgAmount, threshold);

        // Tutar threshold'dan büyükse tetikle
        if (amount.compareTo(threshold) > 0) {
            double actualMultiplier = amount.divide(avgAmount, 2, java.math.RoundingMode.HALF_UP).doubleValue();

            this.reason = String.format("Amount (%.2f) is %.1fx customer average (%.2f)",
                    amount.doubleValue(), actualMultiplier, avgAmount.doubleValue());

            logger.info("HIGH AMOUNT RULE TRIGGERED - Transaction: {}, Reason: {}",
                    transactionId, this.reason);

            return 30;  // 30 puan ekle
        }

        return 0;  // Kural tetiklenmedi
    }

    @Override
    public String getRuleName() {
        return "high_amount";
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    @Override
    public int getPriority() {
        return 7;  // Yüksek öncelik
    }
}
