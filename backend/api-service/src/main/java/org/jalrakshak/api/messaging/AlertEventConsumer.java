package org.jalrakshak.api.messaging;

import org.jalrakshak.api.service.AlertService;
import org.jalrakshak.shared.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes {@link AlertEvent}s published by the stream processor to
 * {@code river.alerts.v1} and persists them via {@link AlertService}, making
 * them available through the alert REST API.
 *
 * <p>This is the single consumer of {@code river.alerts.v1} in the system --
 * the stream-processor module's earlier placeholder listener (which only
 * logged events) has been removed so there is exactly one processing path
 * for alert persistence.</p>
 */
@Service
public class AlertEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlertEventConsumer.class);

    private final AlertService alertService;

    public AlertEventConsumer(AlertService alertService) {
        this.alertService = alertService;
    }

    @KafkaListener(
            topics = "${kafka.topic.alerts:river.alerts.v1}",
            groupId = "${spring.kafka.consumer.group-id:jalrakshak-alert-persistence}")
    public void consumeAlertEvent(AlertEvent alertEvent) {
        logger.info("Received alert event {} for station {} rule {}",
                alertEvent.getId(), alertEvent.getStationId(), alertEvent.getRuleCode());
        boolean persisted = alertService.persistAlertEvent(alertEvent);
        if (!persisted) {
            logger.warn("Alert event {} was not persisted (see previous warning for reason)", alertEvent.getId());
        }
    }
}
