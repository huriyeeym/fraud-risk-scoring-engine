package com.fraud.risk.kafka;

import com.fraud.risk.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Alert Kafka Producer
 * Yüksek riskli transaction'lar için alert gönderir
 */
@Component
public class AlertProducer {

    private static final Logger logger = LoggerFactory.getLogger(AlertProducer.class);
    private final KafkaTemplate<String, Alert> kafkaTemplate;

    @Value("${fraud.kafka.topic.alerts}")
    private String alertsTopic;

    public AlertProducer(KafkaTemplate<String, Alert> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAlert(Alert alert) {
        try {
            logger.info("Sending alert to Kafka: Transaction={}, Score={}",
                    alert.getTransactionId(), alert.getRiskScore());

            kafkaTemplate.send(alertsTopic, alert.getTransactionId(), alert)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            logger.info("Alert sent successfully: {}", alert.getTransactionId());
                        } else {
                            logger.error("Failed to send alert: {}", exception.getMessage(), exception);
                        }
                    });
        } catch (Exception e) {
            logger.error("Exception sending alert: {}", e.getMessage(), e);
        }
    }
}
