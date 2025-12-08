package com.fraud.alert.kafka;

import com.fraud.alert.model.Alert;
import com.fraud.alert.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AlertConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlertConsumer.class);
    private final AlertService alertService;

    public AlertConsumer(AlertService alertService) {
        this.alertService = alertService;
    }

    @KafkaListener(
            topics = "${fraud.kafka.topic.alerts}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeAlert(Map<String, Object> alertData) {
        try {
            logger.info("Received alert from Kafka: {}", alertData);

            Alert alert = new Alert();
            alert.setTransactionId((String) alertData.get("transaction_id"));

            Object riskScoreObj = alertData.get("risk_score");
            if (riskScoreObj instanceof Number) {
                alert.setRiskScore(BigDecimal.valueOf(((Number) riskScoreObj).doubleValue()));
            }

            alert.setAlertData(alertData);

            alertService.createAlert(alert);

            logger.info("Alert saved successfully: {}", alert.getTransactionId());

        } catch (Exception e) {
            logger.error("Error processing alert: {}", e.getMessage(), e);
        }
    }
}
