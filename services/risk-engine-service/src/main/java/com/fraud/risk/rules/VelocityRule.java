package com.fraud.risk.rules;

import com.fraud.risk.model.CustomerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================
 * VELOCITY RULE
 * ============================================
 * Ne yapar?
 * - Kısa sürede çok fazla işlem varsa → şüpheli
 *
 * Örnek:
 * - Son 10 dakikada 5'ten fazla işlem → 40 puan
 *
 * Neden?
 * - Çalıntı kart: Hızlıca birçok alışveriş yapılır
 * - Gerçek kullanıcı: 10 dakikada 5 alışveriş yapmaz
 *
 * Not: Bu basit implementasyon
 * Gerçekte: Database'den son N dakikadaki transaction'ları saymalı
 */
@Component
public class VelocityRule implements FraudRule {

    private static final Logger logger = LoggerFactory.getLogger(VelocityRule.class);
    private String reason;

    // Not: Gerçek implementasyon için TransactionRepository inject edilmeli
    // Bu basit versiyonda sadece örnek gösteriyoruz

    @Override
    public int evaluate(
            String transactionId,
            String customerId,
            BigDecimal amount,
            String merchantCategory,
            String location,
            LocalDateTime timestamp,
            CustomerProfile customerProfile) {

        // TODO: Gerçek implementasyon
        // 1. Database'den son 10 dakikadaki transaction'ları say
        // 2. 5'ten fazlaysa 40 puan döner
        // 3. Şimdilik placeholder

        logger.debug("Velocity Rule - Customer: {} (Placeholder - TODO)", customerId);

        // Placeholder: Her zaman 0 döner (henüz implement edilmedi)
        return 0;
    }

    @Override
    public String getRuleName() {
        return "velocity";
    }

    @Override
    public String getReason() {
        return this.reason != null ? this.reason : "Velocity check passed";
    }

    @Override
    public int getPriority() {
        return 9;  // En yüksek öncelik (critical)
    }
}
