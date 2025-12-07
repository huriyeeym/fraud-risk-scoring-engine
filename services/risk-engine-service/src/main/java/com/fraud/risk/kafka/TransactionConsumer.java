package com.fraud.risk.kafka;

import com.fraud.risk.model.Transaction;
import com.fraud.risk.service.RiskEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * ============================================
 * TRANSACTION KAFKA CONSUMER
 * ============================================
 * Ne yapar?
 * - Kafka'dan transaction'ları dinler
 * - Risk Engine Service'e gönderir
 * - Manual commit (güvenli)
 */
@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private final RiskEngineService riskEngineService;

    public TransactionConsumer(RiskEngineService riskEngineService) {
        this.riskEngineService = riskEngineService;
    }

    /**
     * Kafka'dan transaction dinle
     * @KafkaListener: Bu metod Kafka consumer
     */
    @KafkaListener(
            topics = "${fraud.kafka.topic.transactions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransaction(Transaction transaction, Acknowledgment acknowledgment) {
        try {
            logger.info("Received transaction from Kafka: ID={}, Customer={}, Amount={}",
                    transaction.getTransactionId(),
                    transaction.getCustomerId(),
                    transaction.getAmount());

            // Risk Engine'e gönder (fraud analizi yap)
            riskEngineService.analyzeTransaction(transaction);

            // Manuel commit (başarılı işlendi)
            acknowledgment.acknowledge();

            logger.info("Transaction processed successfully: ID={}", transaction.getTransactionId());

        } catch (Exception e) {
            logger.error("Error processing transaction: ID={}, Error: {}",
                    transaction != null ? transaction.getTransactionId() : "null",
                    e.getMessage(), e);
            // Not: Hata olursa commit yapma (retry edilsin)
        }
    }
}
