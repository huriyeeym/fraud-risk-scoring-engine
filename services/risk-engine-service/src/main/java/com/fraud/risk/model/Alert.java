package com.fraud.risk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Alert DTO - Kafka'ya gönderilecek mesaj
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private BigDecimal riskScore;
    private Map<String, Object> reasons;
    private LocalDateTime timestamp;
}
