package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertStatus;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.mapper.AlertEventMapper;
import org.jalrakshak.api.mapper.AlertMapper;
import org.jalrakshak.api.repository.AlertRepository;
import org.jalrakshak.api.repository.StationRepository;
import org.jalrakshak.shared.AlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the alert-persistence idempotency/upsert behavior described in
 * docs/architecture.md: redelivery of the same AlertEvent.id updates the
 * same row, and a repeat occurrence never clobbers an operator's
 * acknowledge/resolve action.
 */
class AlertServiceTest {

    private AlertRepository alertRepository;
    private StationRepository stationRepository;
    private AlertService alertService;

    private final UUID alertId = UUID.randomUUID();
    private final UUID stationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        stationRepository = mock(StationRepository.class);
        AlertMapper alertMapper = mock(AlertMapper.class);
        AlertEventMapper alertEventMapper = new AlertEventMapper();
        alertService = new AlertService(alertRepository, stationRepository, alertMapper, alertEventMapper);

        when(stationRepository.findById(stationId))
                .thenReturn(Optional.of(Station.builder().id(stationId).stationCode("station-001").name("N").build()));
    }

    private AlertEvent sampleEvent(Instant triggeredAt, int repeatCount) {
        return AlertEvent.builder()
                .id(alertId)
                .stationId(stationId)
                .ruleCode("LEVEL_HIGH")
                .severity("HIGH")
                .status("ACTIVE")
                .title("Water level high")
                .reason("Water level 4.2m >= 4.0m")
                .evidence(Map.of("triggeringValue", 4.2, "thresholdValue", 4.0))
                .triggeredAt(triggeredAt)
                .lastOccurredAt(triggeredAt)
                .repeatCount(repeatCount)
                .build();
    }

    @Test
    void firstOccurrenceCreatesNewActiveAlertWithTheEventId() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        boolean persisted = alertService.persistAlertEvent(sampleEvent(Instant.parse("2026-09-26T10:00:00Z"), 1));

        assertTrue(persisted);
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert saved = captor.getValue();
        assertEquals(alertId, saved.getId());
        assertEquals(AlertStatus.ACTIVE, saved.getStatus());
        assertEquals(1, saved.getRepeatCount());
    }

    @Test
    void redeliveryOfSameEventIdUpdatesExistingRowInsteadOfDuplicating() {
        Alert existing = Alert.builder()
                .id(alertId)
                .station(Station.builder().id(stationId).build())
                .status(AlertStatus.ACTIVE)
                .repeatCount(1)
                .triggeredAt(Instant.parse("2026-09-26T10:00:00Z"))
                .build();
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(existing));

        boolean persisted = alertService.persistAlertEvent(sampleEvent(Instant.parse("2026-09-26T10:00:00Z"), 1));

        assertTrue(persisted);
        // Redelivery of the identical event must not create a second row --
        // save() is called with the SAME (mutated) existing instance, and
        // findById is never followed by any kind of "create new" path.
        verify(alertRepository, times(1)).save(existing);
    }

    @Test
    void repeatOccurrenceUpdatesEvidenceAndRepeatCountButPreservesOperatorAcknowledgement() {
        Alert existing = Alert.builder()
                .id(alertId)
                .station(Station.builder().id(stationId).build())
                .status(AlertStatus.ACKNOWLEDGED)
                .acknowledgedAt(Instant.parse("2026-09-26T10:05:00Z"))
                .repeatCount(1)
                .triggeredAt(Instant.parse("2026-09-26T10:00:00Z"))
                .build();
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(existing));

        boolean persisted = alertService.persistAlertEvent(sampleEvent(Instant.parse("2026-09-26T10:20:00Z"), 2));

        assertTrue(persisted);
        assertEquals(AlertStatus.ACKNOWLEDGED, existing.getStatus(), "operator's acknowledgement must be preserved");
        assertEquals(Instant.parse("2026-09-26T10:05:00Z"), existing.getAcknowledgedAt());
        assertEquals(2, existing.getRepeatCount(), "repeat count must be refreshed");
        assertEquals(Instant.parse("2026-09-26T10:20:00Z"), existing.getLastOccurredAt());
    }

    @Test
    void unknownStationCausesEventToBeSkippedNotPersisted() {
        UUID unknownStation = UUID.randomUUID();
        when(stationRepository.findById(unknownStation)).thenReturn(Optional.empty());
        AlertEvent event = sampleEvent(Instant.now(), 1).toBuilder().stationId(unknownStation).build();

        boolean persisted = alertService.persistAlertEvent(event);

        assertFalse(persisted);
        verify(alertRepository, never()).save(any());
    }
}
