package com.fraud.transaction.kafka;

import com.fraud.transaction.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * ============================================
 * TRANSACTION KAFKA PRODUCER
 * ============================================
 * Ne yapar?
 * - Transaction'ları Kafka topic'ine gönderir
 * - Risk Engine Service bu topic'i dinler
 *
 * Kafka Producer nedir?
 * - Mesaj gönderen taraf
 * - Consumer (alıcı) ile asenkron iletişim
 *
 * Neden Kafka?
 * - Loose coupling: Servisler birbirini bilmez
 * - Scalability: 1000 transaction/saniye destekler
 * - Reliability: Mesaj kaybolmaz (persistence)
 */
@Component
public class TransactionProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);

    // KafkaTemplate: Spring'in Kafka client wrapper'ı
    // <String, Transaction>: Key tipi, Value tipi
    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    // Topic adı application.yml'den gelir
    @Value("${fraud.kafka.topic.transactions}")
    private String transactionsTopic;

    public TransactionProducer(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * ============================================
     * SEND TRANSACTION TO KAFKA
     * ============================================
     * Ne yapar?
     * 1. Transaction'ı JSON'a serialize eder
     * 2. Kafka topic'ine gönderir
     * 3. Asenkron çalışır (bekleme yok)
     *
     * @param transaction Gönderilecek transaction
     */
    public void sendTransaction(Transaction transaction) {
        try {
            logger.info("Sending transaction to Kafka topic: {}", transactionsTopic);
            logger.debug("Transaction details: ID={}, Customer={}, Amount={}",
                    transaction.getTransactionId(),
                    transaction.getCustomerId(),
                    transaction.getAmount());

            // Kafka'ya gönder
            // Key: transaction_id (aynı transaction_id hep aynı partition'a gider)
            // Value: transaction object (JSON'a serialize edilir)
            CompletableFuture<SendResult<String, Transaction>> future =
                    kafkaTemplate.send(transactionsTopic, transaction.getTransactionId(), transaction);

            // ============================================
            // CALLBACK (İşlem tamamlandığında)
            // ============================================
            // Neden callback? Asenkron olduğu için sonucu hemen bilemiyoruz
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    // Başarılı!
                    logger.info("Transaction sent successfully: ID={}, Partition={}, Offset={}",
                            transaction.getTransactionId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    // Hata!
                    logger.error("Failed to send transaction: ID={}, Error: {}",
                            transaction.getTransactionId(),
                            exception.getMessage(), exception);
                }
            });

        } catch (Exception e) {
            logger.error("Exception while sending transaction to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send transaction to Kafka", e);
        }
    }

    /**
     * ============================================
     * SEND TRANSACTION (SYNCHRONOUS)
     * ============================================
     * Ne yapar?
     * - Kafka'ya gönderir VE sonucu bekler (blocking)
     *
     * Neden kullanılır?
     * - Test ortamında (sonucun başarılı olduğunu garantilemek için)
     * - Kritik işlemlerde (mutlaka gönderilmeli)
     *
     * Neden tercih edilmez?
     * - Yavaş (blocking)
     * - Throughput düşer
     *
     * @param transaction Gönderilecek transaction
     */
    public void sendTransactionSync(Transaction transaction) {
        try {
            logger.info("Sending transaction synchronously: ID={}", transaction.getTransactionId());

            SendResult<String, Transaction> result =
                    kafkaTemplate.send(transactionsTopic, transaction.getTransactionId(), transaction)
                            .get();  // .get() = BLOCK until complete

            logger.info("Transaction sent successfully (sync): Partition={}, Offset={}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            logger.error("Failed to send transaction synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send transaction to Kafka", e);
        }
    }
}
