package com.fraud.alert.service;

import com.fraud.alert.model.Alert;
import com.fraud.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert createAlert(Alert alert) {
        logger.info("Creating alert: Transaction={}, RiskScore={}",
                alert.getTransactionId(), alert.getRiskScore());

        Alert savedAlert = alertRepository.save(alert);

        logger.info("Alert created: ID={}", savedAlert.getId());
        return savedAlert;
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<Alert> getAlertsByStatus(Alert.AlertStatus status) {
        return alertRepository.findByStatus(status);
    }

    @Transactional
    public Alert updateAlertStatus(Long id, Alert.AlertStatus status, String reviewedBy, String notes) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

        alert.setStatus(status);
        alert.setReviewedBy(reviewedBy);
        alert.setNotes(notes);
        alert.setReviewedAt(java.time.LocalDateTime.now());

        return alertRepository.save(alert);
    }
}
