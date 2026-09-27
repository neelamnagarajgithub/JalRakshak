package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertStatus;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.AlertDto;
import org.jalrakshak.api.mapper.AlertEventMapper;
import org.jalrakshak.api.mapper.AlertMapper;
import org.jalrakshak.api.repository.AlertRepository;
import org.jalrakshak.api.repository.StationRepository;
import org.jalrakshak.shared.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final StationRepository stationRepository;
    private final AlertMapper alertMapper;
    private final AlertEventMapper alertEventMapper;

    public AlertService(AlertRepository alertRepository,
                         StationRepository stationRepository,
                         AlertMapper alertMapper,
                         AlertEventMapper alertEventMapper) {
        this.alertRepository = alertRepository;
        this.stationRepository = stationRepository;
        this.alertMapper = alertMapper;
        this.alertEventMapper = alertEventMapper;
    }

    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    /**
     * Persists an {@link AlertEvent} consumed from {@code river.alerts.v1}.
     *
     * <p>Idempotency / redelivery: {@code event.getId()} is used as the
     * primary key, so re-processing the same Kafka record (e.g. after a
     * consumer restart before the offset was committed) simply re-applies
     * the same field values to the same row rather than creating a
     * duplicate. This provides at-least-once delivery with an idempotent
     * write, which is a weaker guarantee than end-to-end exactly-once: two
     * different alert ids for what a human would consider "the same"
     * condition are only avoided because the stream processor's suppression
     * function (see stream-processor docs) deliberately reuses one id for
     * the life of a suppression window.</p>
     *
     * <p>If the alert already exists and has since been acknowledged or
     * resolved by an operator, that lifecycle state is preserved -- only
     * evidence/repeat-count/lastOccurredAt are refreshed, never
     * status/acknowledgedAt/resolvedAt.</p>
     *
     * @return true if the event was persisted, false if it was skipped
     *         (e.g. unknown station)
     */
    @Transactional
    public boolean persistAlertEvent(AlertEvent event) {
        if (event.getId() == null || event.getStationId() == null) {
            logger.warn("Discarding alert event with missing id/stationId: {}", event);
            return false;
        }

        Optional<Station> station = stationRepository.findById(event.getStationId());
        if (station.isEmpty()) {
            // Stations are expected to already be registered before they can
            // produce alerts. We do not fabricate a station record here --
            // instead we log and skip, so an operator can investigate rather
            // than getting an alert with a synthetic/incomplete station.
            logger.warn("Discarding alert event {} for unknown station {}", event.getId(), event.getStationId());
            return false;
        }

        Optional<Alert> existing = alertRepository.findById(event.getId());
        if (existing.isPresent()) {
            alertEventMapper.applyRepeatOccurrence(existing.get(), event);
            alertRepository.save(existing.get());
        } else {
            Alert alert = alertEventMapper.toNewEntity(event, station.get());
            alertRepository.save(alert);
        }
        return true;
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