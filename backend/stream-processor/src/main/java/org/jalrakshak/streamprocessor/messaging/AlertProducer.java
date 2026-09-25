package org.jalrakshak.streamprocessor.messaging;

import org.jalrakshak.shared.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for alert events.
 * In a full implementation, this would be used by Flink to send alert events.
 */
@Service
public class AlertProducer {

    private static final Logger logger = LoggerFactory.getLogger(AlertProducer.class);

    @Value("${kafka.topic.alerts:river.alerts.v1}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AlertProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAlertEvent(AlertEvent alertEvent) {
        String key = alertEvent.getStationId().toString();
        logger.info("Sending alert event to Kafka topic {} with key {}", topic, key);
        kafkaTemplate.send(topic, key, alertEvent);
    }
}