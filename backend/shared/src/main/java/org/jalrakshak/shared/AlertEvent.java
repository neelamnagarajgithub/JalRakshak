package org.jalrakshak.shared;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Shared event definition for alert events, published by the stream
 * processor to {@code river.alerts.v1} and consumed by the API service's
 * alert-persistence consumer.
 *
 * <p>{@code id} is assigned by the stream processor and is stable across
 * repeat occurrences of the same station+rule condition while it remains
 * inside the suppression window (see {@code AlertSuppressionFunction}).
 * The persistence consumer uses this id as the idempotency / upsert key,
 * so redelivery of the same id is safe (it updates the same row) and a
 * repeat occurrence after re-arming is safe (it also reuses the same id
 * while suppressed, or gets a fresh id once suppression expires).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AlertEvent {
    private UUID id;
    private UUID stationId;
    private String ruleCode;
    private String severity;
    private String status;
    private String title;
    private String reason;
    private Map<String, Object> evidence;
    private Instant triggeredAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;

    /** When this same station+rule condition was last observed (re-)triggering, event-time. */
    private Instant lastOccurredAt;

    /** Number of times this condition has (re-)triggered while suppressed/tracked, starting at 1. */
    private Integer repeatCount;
}