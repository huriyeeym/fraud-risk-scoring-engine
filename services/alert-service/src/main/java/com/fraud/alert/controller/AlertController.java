package com.fraud.alert.controller;

import com.fraud.alert.model.Alert;
import com.fraud.alert.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable Alert.AlertStatus status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Alert> updateStatus(
            @PathVariable Long id,
            @RequestParam Alert.AlertStatus status,
            @RequestParam(required = false) String reviewedBy,
            @RequestParam(required = false) String notes) {

        Alert updated = alertService.updateAlertStatus(id, status, reviewedBy, notes);
        return ResponseEntity.ok(updated);
    }
}
