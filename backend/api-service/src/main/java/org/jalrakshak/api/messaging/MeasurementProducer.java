package org.jalrakshak.api.messaging;

import org.jalrakshak.shared.MeasurementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes {@link MeasurementEvent} (the shared, stream-processor-facing
 * DTO) to Kafka. This intentionally never accepts the JPA {@code Measurement}
 * entity: publishing the entity would leak a full {@code Station} object
 * graph and persistence fields onto the wire (see docs/architecture.md,
 * "Kafka event contract").
 */
@Service
public class MeasurementProducer {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementProducer.class);

    @Value("${kafka.topic.measurements:river.measurements.v1}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MeasurementProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMeasurement(MeasurementEvent event) {
        String key = event.getStationId() != null ? event.getStationId().toString() : null;
        logger.info("Sending measurement event {} to Kafka topic {} with key {}", event.getEventId(), topic, key);
        kafkaTemplate.send(topic, key, event);
    }
}
