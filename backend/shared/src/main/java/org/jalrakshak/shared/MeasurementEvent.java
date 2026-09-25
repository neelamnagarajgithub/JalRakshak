package org.jalrakshak.shared;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared event definition for Kafka messaging.
 * This represents the event that gets published to Kafka topics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementEvent {
    private String eventId;
    private UUID stationId;
    private Instant observedAt;
    private Double waterLevelM;
    private Double rainfallMmPerHour;
    private Double flowRateM3S;
    private Integer schemaVersion;
}