package com.fraud.risk.rules;

import com.fraud.risk.model.CustomerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================
 * LOCATION ANOMALY RULE
 * ============================================
 * Ne yapar?
 * - İşlem, müşterinin sık kullandığı lokasyonlardan değilse → şüpheli
 *
 * Örnek:
 * - Müşteri hep Istanbul, Ankara'dan işlem yapıyor
 * - Aniden Antalya'dan işlem geldi → 25 puan
 *
 * Neden?
 * - Çalıntı kart: Farklı şehirden kullanılır
 * - Account takeover: Hacker farklı lokasyondan bağlanır
 */
@Component
public class LocationAnomalyRule implements FraudRule {

    private static final Logger logger = LoggerFactory.getLogger(LocationAnomalyRule.class);
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

        // Profile yoksa veya location bilgisi yoksa skip
        if (customerProfile == null || location == null) {
            return 0;
        }

        // Lokasyon sık kullanılanlar arasında var mı?
        boolean isFrequent = customerProfile.isLocationFrequent(location);

        logger.debug("Location Anomaly Rule - Customer: {}, Location: {}, Is Frequent: {}",
                customerId, location, isFrequent);

        if (!isFrequent) {
            this.reason = String.format("Unusual location: %s (not in frequent locations)", location);

            logger.info("LOCATION ANOMALY RULE TRIGGERED - Transaction: {}, Reason: {}",
                    transactionId, this.reason);

            return 25;  // 25 puan ekle
        }

        return 0;
    }

    @Override
    public String getRuleName() {
        return "location_anomaly";
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    @Override
    public int getPriority() {
        return 6;  // Orta-yüksek öncelik
    }
}
