package org.jalrakshak.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementDto {
    @NotBlank
    private String eventId;

    @NotNull
    private UUID stationId;

    @NotNull
    private Instant observedAt;

    @NotNull
    @PositiveOrZero
    private Double waterLevelM;

    @NotNull
    @PositiveOrZero
    private Double rainfallMmPerHour;

    private Double flowRateM3S;

    @NotNull
    @Positive
    private Integer schemaVersion;
}