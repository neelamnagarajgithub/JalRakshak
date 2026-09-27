package org.jalrakshak.streamprocessor.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jalrakshak.shared.AlertEvent;
import org.jalrakshak.shared.json.EventObjectMapper;

import java.nio.charset.StandardCharsets;

public class AlertEventSerializer implements KafkaRecordSerializationSchema<AlertEvent> {

    private final String topic;
    private transient ObjectMapper mapper;

    public AlertEventSerializer(String topic) {
        this.topic = topic;
    }

    private ObjectMapper mapper() {
        if (mapper == null) {
            mapper = EventObjectMapper.create();
        }
        return mapper;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(AlertEvent element, KafkaSinkContext context, Long timestamp) {
        try {
            byte[] key = element.getStationId() != null
                    ? element.getStationId().toString().getBytes(StandardCharsets.UTF_8)
                    : null;
            byte[] value = mapper().writeValueAsBytes(element);
            return new ProducerRecord<>(topic, null, key, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize AlertEvent " + element.getId(), e);
        }
    }
}