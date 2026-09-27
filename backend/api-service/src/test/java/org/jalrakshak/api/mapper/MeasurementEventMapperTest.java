package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.shared.MeasurementEvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the API publishes only the agreed MeasurementEvent fields -- in
 * particular that stationId (not a nested Station graph or persistence
 * fields like the measurement's own id/receivedAt) ends up on the wire.
 */
class MeasurementEventMapperTest {

    private final MeasurementEventMapper mapper = Mappers.getMapper(MeasurementEventMapper.class);

    @Test
    void mapsStationIdNotFullStationGraph() {
        UUID stationId = UUID.randomUUID();
        Station station = Station.builder()
                .id(stationId)
                .stationCode("station-001")
                .name("North River Station")
                .build();

        Measurement measurement = Measurement.builder()
                .id(UUID.randomUUID())
                .eventId("evt-002")
                .station(station)
                .observedAt(Instant.parse("2026-09-26T10:15:00Z"))
                .receivedAt(Instant.now())
                .waterLevelM(2.5)
                .rainfallMmPerHour(1.0)
                .flowRateM3S(10.0)
                .schemaVersion(1)
                .build();

        MeasurementEvent event = mapper.toEvent(measurement);

        assertEquals(stationId, event.getStationId());
        assertEquals("evt-002", event.getEventId());
        assertEquals(Instant.parse("2026-09-26T10:15:00Z"), event.getObservedAt());
        assertEquals(2.5, event.getWaterLevelM());
        assertEquals(1.0, event.getRainfallMmPerHour());
        assertEquals(10.0, event.getFlowRateM3S());
        assertEquals(1, event.getSchemaVersion());
    }

    @Test
    void nullMeasurementMapsToNull() {
        assertNull(mapper.toEvent(null));
    }
}
