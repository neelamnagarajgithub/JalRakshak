package org.jalrakshak.streamprocessor.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.shared.json.EventObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Deserializes {@code river.measurements.v1} records into
 * {@link MeasurementIngestResult}.
 *
 * <p>Malformed-event policy (documented per the brief's requirement to be
 * explicit about this): a record that is not valid JSON, does not match the
 * MeasurementEvent shape, or is missing a required field
 * (eventId/stationId/observedAt/schemaVersion) is <b>never</b> thrown as an
 * exception that would crash the source (which would stop the whole job on
 * one bad record) and is <b>never</b> silently dropped. It is wrapped as a
 * {@code MeasurementIngestResult.malformed(...)} and collected like any
 * other record; a downstream {@code ProcessFunction} splits valid events
 * into the main pipeline and routes malformed ones to a side output that is
 * logged (see {@code StreamProcessorJob}).</p>
 */
public class MeasurementEventDeserializer implements KafkaRecordDeserializationSchema<MeasurementIngestResult> {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementEventDeserializer.class);

    private transient ObjectMapper mapper;

    private ObjectMapper mapper() {
        if (mapper == null) {
            mapper = EventObjectMapper.create();
        }
        return mapper;
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<MeasurementIngestResult> out) {
        byte[] value = record.value();
        String raw = value == null ? null : new String(value, StandardCharsets.UTF_8);

        if (raw == null || raw.isBlank()) {
            out.collect(MeasurementIngestResult.malformed(raw, "empty record value"));
            return;
        }

        try {
            MeasurementEvent event = mapper().readValue(raw, MeasurementEvent.class);
            String validationError = validate(event);
            if (validationError != null) {
                logger.warn("Malformed measurement event (topic={} partition={} offset={}): {}",
                        record.topic(), record.partition(), record.offset(), validationError);
                out.collect(MeasurementIngestResult.malformed(raw, validationError));
                return;
            }
            out.collect(MeasurementIngestResult.valid(event));
        } catch (Exception e) {
            logger.warn("Failed to parse measurement event (topic={} partition={} offset={}): {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            out.collect(MeasurementIngestResult.malformed(raw, "JSON parse error: " + e.getMessage()));
        }
    }

    private String validate(MeasurementEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) return "missing eventId";
        if (event.getStationId() == null) return "missing stationId";
        if (event.getObservedAt() == null) return "missing observedAt";
        if (event.getSchemaVersion() == null) return "missing schemaVersion";
        if (event.getWaterLevelM() != null && event.getWaterLevelM() < 0) return "negative waterLevelM";
        if (event.getRainfallMmPerHour() != null && event.getRainfallMmPerHour() < 0) return "negative rainfallMmPerHour";
        return null;
    }

    @Override
    public TypeInformation<MeasurementIngestResult> getProducedType() {
        return TypeInformation.of(MeasurementIngestResult.class);
    }
}