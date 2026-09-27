package org.jalrakshak.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationDto {
    private UUID id;

    @NotBlank
    private String stationCode;

    @NotBlank
    private String name;

    private String riverName;
    private Double latitude;
    private Double longitude;
    private String status;
    private Instant lastSeenAt;
    private Instant createdAt;
}