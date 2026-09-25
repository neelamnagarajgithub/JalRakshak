package org.jalrakshak.shared;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Shared event definition for alert events.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}