package org.jalrakshak.api.messaging;

import org.jalrakshak.api.domain.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeasurementProducer {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementProducer.class);

    @Value("${kafka.topic.measurements:river.measurements.v1}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MeasurementProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMeasurement(Measurement measurement) {
        String key = measurement.getStation().getId().toString();
        logger.info("Sending measurement to Kafka topic {} with key {}", topic, key);
        kafkaTemplate.send(topic, key, measurement);
    }
}