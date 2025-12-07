package com.fraud.transaction.service;

import com.fraud.transaction.kafka.TransactionProducer;
import com.fraud.transaction.model.Transaction;
import com.fraud.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================
 * TRANSACTION SERVICE
 * ============================================
 * Ne yapar?
 * - Transaction business logic
 * - Database'e kayıt
 * - Kafka'ya gönderim
 *
 * Neden Service katmanı?
 * - Controller: HTTP işlemleri (endpoint)
 * - Service: Business logic (validation, orchestration)
 * - Repository: Database işlemleri
 *
 * Separation of Concerns (Görev Ayrımı)!
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;

    // Constructor injection (best practice)
    // Neden? Field injection yerine constructor tercih edilir (testable, immutable)
    public TransactionService(TransactionRepository transactionRepository,
                              TransactionProducer transactionProducer) {
        this.transactionRepository = transactionRepository;
        this.transactionProducer = transactionProducer;
    }

    /**
     * ============================================
     * CREATE TRANSACTION
     * ============================================
     * Ne yapar?
     * 1. Transaction'ı validate eder
     * 2. Database'e kaydeder
     * 3. Kafka'ya gönderir
     *
     * @Transactional nedir?
     * - Database işlemi başarısızsa rollback yapar
     * - ACID garantisi
     *
     * @param transaction Kaydedilecek transaction
     * @return Kaydedilen transaction
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        logger.info("Creating transaction: ID={}, Customer={}, Amount={}",
                transaction.getTransactionId(),
                transaction.getCustomerId(),
                transaction.getAmount());

        // ============================================
        // VALIDATION (Business Rules)
        // ============================================
        validateTransaction(transaction);

        // ============================================
        // SAVE TO DATABASE
        // ============================================
        // save() metodu INSERT veya UPDATE yapar (ID'ye göre)
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction saved to database: ID={}", savedTransaction.getTransactionId());

        // ============================================
        // SEND TO KAFKA
        // ============================================
        // Asenkron gönderim (blocking değil)
        // Risk Engine Service bu mesajı alacak
        try {
            transactionProducer.sendTransaction(savedTransaction);
            logger.info("Transaction sent to Kafka: ID={}", savedTransaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to send transaction to Kafka: {}", e.getMessage(), e);
            // Not: Database'e zaten kaydedildi, Kafka hatası transaction'ı rollback etmez
            // Opsiyonel: Retry mechanism veya Dead Letter Queue eklenebilir
        }

        return savedTransaction;
    }

    /**
     * ============================================
     * VALIDATE TRANSACTION
     * ============================================
     * Ne yapar?
     * - Business rule validation
     * - @Valid annotation'dan sonra ek kontroller
     *
     * Örnek kurallar:
     * - Transaction ID duplicate olmamalı
     * - Amount makul aralıkta olmalı (örn: max 100,000)
     */
    private void validateTransaction(Transaction transaction) {
        // Transaction ID kontrolü (duplicate?)
        if (transactionRepository.existsById(transaction.getTransactionId())) {
            logger.error("Duplicate transaction ID: {}", transaction.getTransactionId());
            throw new IllegalArgumentException("Transaction ID already exists: " + transaction.getTransactionId());
        }

        // Amount kontrolü (makul mu?)
        if (transaction.getAmount().doubleValue() > 100_000) {
            logger.warn("Unusually high transaction amount: ID={}, Amount={}",
                    transaction.getTransactionId(), transaction.getAmount());
            // Warning log, ama reject etmiyoruz (fraud detection engine karar verecek)
        }

        // Timestamp kontrolü (gelecek tarih olamaz)
        if (transaction.getTimestamp() != null &&
            transaction.getTimestamp().isAfter(LocalDateTime.now())) {
            logger.error("Future timestamp not allowed: {}", transaction.getTimestamp());
            throw new IllegalArgumentException("Transaction timestamp cannot be in the future");
        }
    }

    /**
     * ============================================
     * GET TRANSACTION BY ID
     * ============================================
     */
    public Optional<Transaction> getTransactionById(String transactionId) {
        logger.debug("Fetching transaction: ID={}", transactionId);
        return transactionRepository.findById(transactionId);
    }

    /**
     * ============================================
     * GET TRANSACTIONS BY CUSTOMER
     * ============================================
     * Ne yapar?
     * - Belirli bir müşterinin tüm işlemlerini getirir
     * - Customer profiling için kullanılabilir
     */
    public List<Transaction> getTransactionsByCustomer(String customerId) {
        logger.debug("Fetching transactions for customer: {}", customerId);
        return transactionRepository.findByCustomerId(customerId);
    }

    /**
     * ============================================
     * GET RECENT TRANSACTIONS
     * ============================================
     * Ne yapar?
     * - Son N dakikadaki işlemleri getirir
     * - Velocity rule için kullanılır
     *
     * Örnek: Son 10 dakikada 5'ten fazla işlem varsa → fraud şüphesi
     */
    public List<Transaction> getRecentTransactionsByCustomer(String customerId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        logger.debug("Fetching recent transactions: Customer={}, Since={}", customerId, since);
        return transactionRepository.findRecentTransactionsByCustomer(customerId, since);
    }

    /**
     * ============================================
     * GET ALL TRANSACTIONS
     * ============================================
     * Not: Production'da pagination kullanılmalı!
     * findAll() yerine findAll(Pageable) tercih et
     */
    public List<Transaction> getAllTransactions() {
        logger.debug("Fetching all transactions");
        return transactionRepository.findAll();
    }
}
