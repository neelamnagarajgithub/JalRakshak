package org.jalrakshak.streamprocessor.serde;

import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jalrakshak.shared.json.EventObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementEventDeserializerTest {

    /** Minimal in-memory Collector so the deserializer can be exercised without a Flink runtime. */
    private static final class RecordingCollector implements Collector<MeasurementIngestResult> {
        final List<MeasurementIngestResult> collected = new ArrayList<>();
        @Override public void collect(MeasurementIngestResult record) { collected.add(record); }
        @Override public void close() { }
    }

    private final MeasurementEventDeserializer deserializer = new MeasurementEventDeserializer();

    private ConsumerRecord<byte[], byte[]> record(String json) {
        byte[] value = json == null ? null : json.getBytes(StandardCharsets.UTF_8);
        return new ConsumerRecord<>("river.measurements.v1", 0, 0L, new byte[0], value);
    }

    @Test
    void validEventIsCollectedAsValid() throws Exception {
        String json = EventObjectMapper.create().writeValueAsString(
                org.jalrakshak.shared.MeasurementEvent.builder()
                        .eventId("evt-1")
                        .stationId(UUID.randomUUID())
                        .observedAt(java.time.Instant.parse("2026-09-26T10:00:00Z"))
                        .waterLevelM(2.0)
                        .rainfallMmPerHour(1.0)
                        .schemaVersion(1)
                        .build());

        RecordingCollector out = new RecordingCollector();
        deserializer.deserialize(record(json), out);

        assertEquals(1, out.collected.size());
        assertTrue(out.collected.get(0).isValid());
        assertEquals("evt-1", out.collected.get(0).getEvent().getEventId());
    }

    @Test
    void invalidJsonIsRoutedToMalformedNotThrown() {
        RecordingCollector out = new RecordingCollector();
        assertDoesNotThrow(() -> deserializer.deserialize(record("{not valid json"), out));

        assertEquals(1, out.collected.size());
        assertFalse(out.collected.get(0).isValid());
        assertNotNull(out.collected.get(0).getError());
    }

    @Test
    void missingRequiredFieldIsRoutedToMalformed() {
        String json = "{\"waterLevelM\": 2.0}"; // no eventId/stationId/observedAt/schemaVersion
        RecordingCollector out = new RecordingCollector();
        deserializer.deserialize(record(json), out);

        assertEquals(1, out.collected.size());
        assertFalse(out.collected.get(0).isValid());
    }

    @Test
    void negativeWaterLevelIsRejectedAsMalformed() {
        String json = "{\"eventId\":\"evt-2\",\"stationId\":\"" + UUID.randomUUID()
                + "\",\"observedAt\":\"2026-09-26T10:00:00Z\",\"schemaVersion\":1,\"waterLevelM\":-1.0}";
        RecordingCollector out = new RecordingCollector();
        deserializer.deserialize(record(json), out);

        assertEquals(1, out.collected.size());
        assertFalse(out.collected.get(0).isValid());
    }

    @Test
    void emptyRecordValueIsRoutedToMalformedNotThrown() {
        RecordingCollector out = new RecordingCollector();
        assertDoesNotThrow(() -> deserializer.deserialize(record(null), out));
        assertEquals(1, out.collected.size());
        assertFalse(out.collected.get(0).isValid());
    }
}
