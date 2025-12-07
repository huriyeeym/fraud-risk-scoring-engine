package com.fraud.risk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction DTO - Kafka'dan gelen mesaj
 * (Entity değil, sadece data transfer object)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private String merchantCategory;
    private String location;
    private LocalDateTime timestamp;
}
