package com.fraud.transaction.controller;

import com.fraud.transaction.model.Transaction;
import com.fraud.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================
 * TRANSACTION CONTROLLER
 * ============================================
 * Ne yapar?
 * - REST API endpoints sağlar
 * - HTTP isteklerini karşılar
 * - JSON request/response
 *
 * @RestController nedir?
 * - @Controller + @ResponseBody
 * - Dönen değerler otomatik JSON'a çevrilir
 *
 * @RequestMapping("/api/transactions")
 * - Tüm endpoint'ler bu path ile başlar
 * - Örnek: POST /api/transactions
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * ============================================
     * CREATE TRANSACTION
     * ============================================
     * Endpoint: POST /api/transactions
     *
     * Request Body (JSON):
     * {
     *   "transactionId": "T001",
     *   "customerId": "C123",
     *   "amount": 1500.50,
     *   "merchantCategory": "electronics",
     *   "location": "Istanbul"
     * }
     *
     * Response: 201 CREATED + Transaction object
     *
     * @Valid nedir?
     * - Transaction model'deki validation annotation'ları kontrol eder
     * - @NotNull, @DecimalMin, vb.
     * - Geçersizse 400 Bad Request döner
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        logger.info("Received create transaction request: ID={}", transaction.getTransactionId());

        try {
            Transaction createdTransaction = transactionService.createTransaction(transaction);

            logger.info("Transaction created successfully: ID={}", createdTransaction.getTransactionId());

            // 201 Created status code
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);

        } catch (IllegalArgumentException e) {
            // Business validation hatası (duplicate ID, vb.)
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            // Beklenmeyen hata
            logger.error("Error creating transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ============================================
     * GET TRANSACTION BY ID
     * ============================================
     * Endpoint: GET /api/transactions/{id}
     *
     * Örnek: GET /api/transactions/T001
     *
     * Response:
     * - 200 OK + Transaction (varsa)
     * - 404 NOT FOUND (yoksa)
     *
     * @PathVariable nedir?
     * - URL'den parameter alır
     * - {id} placeholder'ından değer çeker
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String id) {
        logger.debug("Fetching transaction: ID={}", id);

        return transactionService.getTransactionById(id)
                .map(transaction -> {
                    logger.debug("Transaction found: ID={}", id);
                    return ResponseEntity.ok(transaction);
                })
                .orElseGet(() -> {
                    logger.warn("Transaction not found: ID={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * ============================================
     * GET TRANSACTIONS BY CUSTOMER
     * ============================================
     * Endpoint: GET /api/transactions/customer/{customerId}
     *
     * Örnek: GET /api/transactions/customer/C123
     *
     * Response: 200 OK + List<Transaction>
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomer(@PathVariable String customerId) {
        logger.debug("Fetching transactions for customer: {}", customerId);

        List<Transaction> transactions = transactionService.getTransactionsByCustomer(customerId);

        logger.debug("Found {} transactions for customer: {}", transactions.size(), customerId);

        return ResponseEntity.ok(transactions);
    }

    /**
     * ============================================
     * GET RECENT TRANSACTIONS BY CUSTOMER
     * ============================================
     * Endpoint: GET /api/transactions/customer/{customerId}/recent?minutes=10
     *
     * Query Parameter: minutes (default: 10)
     *
     * Örnek: GET /api/transactions/customer/C123/recent?minutes=30
     *
     * Response: 200 OK + List<Transaction>
     *
     * @RequestParam nedir?
     * - Query string'den parameter alır
     * - ?minutes=30 → @RequestParam int minutes
     * - defaultValue: Parameter yoksa varsayılan değer
     */
    @GetMapping("/customer/{customerId}/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "10") int minutes) {

        logger.debug("Fetching recent transactions: Customer={}, Minutes={}", customerId, minutes);

        List<Transaction> transactions = transactionService.getRecentTransactionsByCustomer(customerId, minutes);

        logger.debug("Found {} recent transactions for customer: {}", transactions.size(), customerId);

        return ResponseEntity.ok(transactions);
    }

    /**
     * ============================================
     * GET ALL TRANSACTIONS
     * ============================================
     * Endpoint: GET /api/transactions
     *
     * Response: 200 OK + List<Transaction>
     *
     * Not: Production'da pagination eklenmeliProje çok büyüdükçe tüm transaction'ları getirmek yavaşlar!
     * Örnek: GET /api/transactions?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        logger.debug("Fetching all transactions");

        List<Transaction> transactions = transactionService.getAllTransactions();

        logger.debug("Found {} total transactions", transactions.size());

        return ResponseEntity.ok(transactions);
    }

    /**
     * ============================================
     * HEALTH CHECK
     * ============================================
     * Endpoint: GET /api/transactions/health
     *
     * Response: 200 OK + "Transaction Service is running"
     *
     * Neden? Basit bir check endpoint (Actuator dışında)
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transaction Service is running");
    }
}
