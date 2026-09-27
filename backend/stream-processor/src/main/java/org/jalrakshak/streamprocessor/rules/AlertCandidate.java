package org.jalrakshak.streamprocessor.rules;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A rule match, before the suppression function decides whether it becomes
 * a brand-new {@code AlertEvent} or a repeat-occurrence update to an
 * existing one. Deliberately not the same type as the Kafka
 * {@code AlertEvent} DTO: this has no id/repeatCount/status yet -- those are
 * assigned by {@code AlertSuppressionFunction}.
 */
@Getter
@Builder
public class AlertCandidate implements Serializable {
    private final UUID stationId;
    private final String ruleCode;
    private final String severity;
    private final String title;
    private final String reason;
    private final Map<String, Object> evidence;
    /** Event-time of the measurement that caused this candidate to fire. */
    private final Instant eventTime;
}