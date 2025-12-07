package com.fraud.transaction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================
 * TRANSACTION MODEL
 * ============================================
 * Ne yapar?
 * - Banking transaction'ın veri yapısı
 * - Database tablosuna karşılık gelir (JPA Entity)
 * - JSON'a serialize edilir (Kafka mesajı)
 *
 * Neden bu alanlar?
 * - transaction_id: Benzersiz tanımlayıcı
 * - customer_id: Hangi müşteri?
 * - amount: İşlem tutarı
 * - merchant_category: Hangi kategori? (electronics, food, etc.)
 * - location: Nerede yapıldı?
 * - timestamp: Ne zaman?
 */
@Entity
@Table(name = "transactions")
@Data  // Lombok: getter, setter, toString, equals, hashCode otomatik
@NoArgsConstructor  // Parametresiz constructor
@AllArgsConstructor  // Tüm parametreli constructor
public class Transaction {

    // ============================================
    // PRIMARY KEY
    // ============================================
    @Id
    @Column(name = "transaction_id", length = 50)
    @NotBlank(message = "Transaction ID cannot be blank")
    private String transactionId;

    // ============================================
    // CUSTOMER INFORMATION
    // ============================================
    @Column(name = "customer_id", length = 50, nullable = false)
    @NotBlank(message = "Customer ID cannot be blank")
    private String customerId;

    // ============================================
    // TRANSACTION DETAILS
    // ============================================
    // Amount: DECIMAL(10,2) → Maksimum 99,999,999.99
    // Neden BigDecimal? Double/Float finansal hesaplarda yanlış sonuç verir!
    // Örnek: 0.1 + 0.2 = 0.30000000000000004 (Double)
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;

    @Column(name = "location", length = 100)
    private String location;

    // ============================================
    // TIMESTAMP
    // ============================================
    // LocalDateTime: Java 8+ time API (daha iyi Date sınıfından)
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================
    // Ne yapar? Entity kaydedilmeden önce otomatik çalışır
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }
}
