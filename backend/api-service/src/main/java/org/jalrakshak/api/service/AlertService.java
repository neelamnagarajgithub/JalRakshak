package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertStatus;
import org.jalrakshak.api.dto.AlertDto;
import org.jalrakshak.api.mapper.AlertMapper;
import org.jalrakshak.api.repository.AlertRepository;
import org.jalrakshak.api.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final StationRepository stationRepository;
    private final AlertMapper alertMapper;

    public AlertService(AlertRepository alertRepository, StationRepository stationRepository, AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.stationRepository = stationRepository;
        this.alertMapper = alertMapper;
    }

    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    public Optional<Alert> getAlertById(UUID id) {
        return alertRepository.findById(id);
    }

    public List<AlertDto> getActiveAlerts() {
        return alertRepository.findByStatusOrderByTriggeredAtDesc(AlertStatus.ACTIVE)
                .stream()
                .map(alertMapper::toDto)
                .toList();
    }

    public List<AlertDto> getAlertsByStationId(UUID stationId) {
        return alertRepository.findByStationIdOrderByTriggeredAtDesc(stationId)
                .stream()
                .map(alertMapper::toDto)
                .toList();
    }

    public AlertDto acknowledgeAlert(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new RuntimeException("Only active alerts can be acknowledged");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        Alert savedAlert = alertRepository.save(alert);
        return alertMapper.toDto(savedAlert);
    }

    public AlertDto resolveAlert(UUID id, String note) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new RuntimeException("Alert is already resolved");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        // In a real implementation, we might add the note to evidence or a separate field
        Alert savedAlert = alertRepository.save(alert);
        return alertMapper.toDto(savedAlert);
    }
    public Optional<AlertDto> getAlertDtoById(UUID id) {
        return alertRepository.findById(id)
                .map(alertMapper::toDto);
    }

}