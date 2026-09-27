package org.jalrakshak.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jalrakshak.shared.json.EventObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSerializationTest {

    private final ObjectMapper mapper = EventObjectMapper.create();

    @Test
    void measurementEventRoundTripsThroughJson() throws Exception {
        MeasurementEvent original = MeasurementEvent.builder()
                .eventId("evt-002")
                .stationId(UUID.randomUUID())
                .observedAt(Instant.parse("2026-09-26T10:15:00Z"))
                .waterLevelM(2.5)
                .rainfallMmPerHour(1.0)
                .flowRateM3S(10.0)
                .schemaVersion(1)
                .build();

        String json = mapper.writeValueAsString(original);
        MeasurementEvent parsed = mapper.readValue(json, MeasurementEvent.class);

        assertEquals(original.getEventId(), parsed.getEventId());
        assertEquals(original.getStationId(), parsed.getStationId());
        assertEquals(original.getObservedAt(), parsed.getObservedAt());
        assertEquals(original.getWaterLevelM(), parsed.getWaterLevelM());
        assertEquals(original.getSchemaVersion(), parsed.getSchemaVersion());
    }

    @Test
    void measurementEventTimestampsAreIso8601NotEpochArrays() throws Exception {
        MeasurementEvent event = MeasurementEvent.builder()
                .eventId("evt-1")
                .stationId(UUID.randomUUID())
                .observedAt(Instant.parse("2026-09-26T10:15:00Z"))
                .waterLevelM(1.0)
                .rainfallMmPerHour(0.0)
                .flowRateM3S(1.0)
                .schemaVersion(1)
                .build();

        String json = mapper.writeValueAsString(event);
        assertTrue(json.contains("\"2026-09-26T10:15:00Z\""),
                "expected ISO-8601 instant in JSON, got: " + json);
    }

    @Test
    void alertEventRoundTripsIncludingEvidenceAndRepeatTracking() throws Exception {
        AlertEvent original = AlertEvent.builder()
                .id(UUID.randomUUID())
                .stationId(UUID.randomUUID())
                .ruleCode("LEVEL_HIGH")
                .severity("HIGH")
                .status("ACTIVE")
                .title("Water level high")
                .reason("Water level 4.20m >= 4.00m threshold")
                .evidence(Map.of(
                        "triggeringValue", 4.20,
                        "thresholdValue", 4.00,
                        "eventTimestamp", "2026-09-26T10:15:00Z"
                ))
                .triggeredAt(Instant.parse("2026-09-26T10:15:00Z"))
                .lastOccurredAt(Instant.parse("2026-09-26T10:15:00Z"))
                .repeatCount(1)
                .build();

        String json = mapper.writeValueAsString(original);
        AlertEvent parsed = mapper.readValue(json, AlertEvent.class);

        assertEquals(original.getId(), parsed.getId());
        assertEquals(original.getRuleCode(), parsed.getRuleCode());
        assertEquals(original.getRepeatCount(), parsed.getRepeatCount());
        assertEquals(4.20, (Double) parsed.getEvidence().get("triggeringValue"));
    }

    @Test
    void unknownFieldsAreIgnoredForForwardCompatibility() throws Exception {
        String jsonWithExtraField = "{" +
                "\"eventId\":\"evt-9\"," +
                "\"stationId\":\"" + UUID.randomUUID() + "\"," +
                "\"observedAt\":\"2026-09-26T10:15:00Z\"," +
                "\"waterLevelM\":1.0," +
                "\"rainfallMmPerHour\":0.0," +
                "\"flowRateM3S\":1.0," +
                "\"schemaVersion\":1," +
                "\"someFutureField\":\"ignored\"" +
                "}";

        MeasurementEvent parsed = mapper.readValue(jsonWithExtraField, MeasurementEvent.class);
        assertEquals("evt-9", parsed.getEventId());
    }
}
