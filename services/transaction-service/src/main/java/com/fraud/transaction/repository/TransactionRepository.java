package com.fraud.transaction.repository;

import com.fraud.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================
 * TRANSACTION REPOSITORY
 * ============================================
 * Ne yapar?
 * - Database işlemleri için interface
 * - Spring Data JPA otomatik implementation yapar!
 *
 * JpaRepository nedir?
 * - Hazır metodlar: save(), findById(), findAll(), delete()
 * - SQL yazmana gerek yok!
 *
 * Generic tipler: <Transaction, String>
 * - Transaction: Entity tipi
 * - String: Primary key tipi (transaction_id VARCHAR)
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // ============================================
    // CUSTOM QUERY METHODS
    // ============================================
    // Spring Data JPA metod isminden SQL üretir!

    /**
     * Belirli bir müşterinin tüm işlemlerini getir
     * SQL: SELECT * FROM transactions WHERE customer_id = ?
     */
    List<Transaction> findByCustomerId(String customerId);

    /**
     * Belirli bir tarih aralığındaki işlemleri getir
     * SQL: SELECT * FROM transactions WHERE timestamp BETWEEN ? AND ?
     */
    List<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Belirli bir müşterinin son N dakikadaki işlemlerini getir
     * Neden? Velocity rule için gerekli!
     * Örnek: Son 10 dakikada 5'ten fazla işlem varsa → fraud şüphesi
     */
    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId " +
           "AND t.timestamp >= :since ORDER BY t.timestamp DESC")
    List<Transaction> findRecentTransactionsByCustomer(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since
    );

    /**
     * Belirli bir lokasyondaki işlemleri getir
     * SQL: SELECT * FROM transactions WHERE location = ?
     */
    List<Transaction> findByLocation(String location);

    /**
     * Belirli bir merchant kategorisindeki işlemleri getir
     * SQL: SELECT * FROM transactions WHERE merchant_category = ?
     */
    List<Transaction> findByMerchantCategory(String merchantCategory);

    /**
     * Belirli bir tutarın üzerindeki işlemleri getir
     * SQL: SELECT * FROM transactions WHERE amount > ?
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount > :amount ORDER BY t.amount DESC")
    List<Transaction> findHighValueTransactions(@Param("amount") java.math.BigDecimal amount);
}
