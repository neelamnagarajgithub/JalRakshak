package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertSeverity;
import org.jalrakshak.api.domain.AlertStatus;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.shared.AlertEvent;
import org.springframework.stereotype.Component;

/**
 * Explicit mapping from the Kafka {@link AlertEvent} DTO produced by the
 * stream processor into the persisted {@link Alert} entity.
 *
 * <p>Deliberately not a MapStruct interface: the target entity's
 * {@code id}/{@code station} associations and the "don't clobber an
 * operator's acknowledge/resolve" merge rule (see
 * {@code AlertService#persistAlertEvent}) both need hand-written logic, so a
 * generated 1:1 mapper would not be the right tool here.</p>
 */
@Component
public class AlertEventMapper {

    /** Builds a brand-new Alert row (first time this station+rule condition is seen). */
    public Alert toNewEntity(AlertEvent event, Station station) {
        Alert alert = new Alert();
        alert.setId(event.getId());
        alert.setStation(station);
        applyEventFields(alert, event);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setTriggeredAt(event.getTriggeredAt());
        return alert;
    }

    /**
     * Applies a repeat occurrence of the same condition onto an existing,
     * already-persisted Alert row. Evidence, severity/title/reason,
     * lastOccurredAt and repeatCount are refreshed; {@code status},
     * {@code acknowledgedAt} and {@code resolvedAt} are intentionally left
     * untouched so an operator's acknowledge/resolve action on this alert is
     * never silently overwritten by a redelivered or re-triggered event.
     */
    public void applyRepeatOccurrence(Alert existing, AlertEvent event) {
        applyEventFields(existing, event);
    }

    private void applyEventFields(Alert alert, AlertEvent event) {
        alert.setRuleCode(event.getRuleCode());
        alert.setSeverity(parseSeverity(event.getSeverity()));
        alert.setTitle(event.getTitle());
        alert.setReason(event.getReason());
        alert.setEvidence(event.getEvidence());
        alert.setLastOccurredAt(event.getLastOccurredAt() != null ? event.getLastOccurredAt() : event.getTriggeredAt());
        alert.setRepeatCount(event.getRepeatCount() != null ? event.getRepeatCount() : 1);
    }

    private AlertSeverity parseSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return AlertSeverity.WARNING;
        }
        try {
            return AlertSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AlertSeverity.WARNING;
        }
    }
}
