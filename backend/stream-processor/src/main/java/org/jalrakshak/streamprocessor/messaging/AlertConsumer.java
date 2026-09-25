package org.jalrakshak.streamprocessor.messaging;

import org.jalrakshak.shared.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for alert events.
 * In a full implementation, this would persist alerts to the database.
 */
@Service
public class AlertConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlertConsumer.class);

    @KafkaListener(topics = "river.alerts.v1", groupId = "jalrakshak-alert-consumer")
    public void consumeAlertEvent(AlertEvent alertEvent) {
        logger.info("Received alert event: {}", alertEvent);
        // In a full implementation, we would:
        // 1. Persist the alert event to the database
        // 2. Update the alert record with the event data
    }
}