package org.jalrakshak.api.controller;

import org.jalrakshak.api.dto.AlertDto;
import org.jalrakshak.api.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertDto> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertDto> getAlertById(@PathVariable("id") String id) {
        try {
            return alertService.getAlertDtoById(UUID.fromString(id))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/station/{stationId}")
    public List<AlertDto> getAlertsByStation(@PathVariable("stationId") String stationId) {
        return alertService.getAlertsByStationId(UUID.fromString(stationId));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable("id") String id) {
        try {
            AlertDto acknowledgedAlert = alertService.acknowledgeAlert(UUID.fromString(id));
            return ResponseEntity.ok(acknowledgedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable("id") String id, @RequestParam(name = "note", required = false) String note) {
        try {
            AlertDto resolvedAlert = alertService.resolveAlert(UUID.fromString(id), note);
            return ResponseEntity.ok(resolvedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}