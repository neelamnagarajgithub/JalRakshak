package org.jalrakshak.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto {
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