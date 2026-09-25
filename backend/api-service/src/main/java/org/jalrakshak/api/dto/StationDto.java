package org.jalrakshak.api.dto;

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
    private String stationCode;
    private String name;
    private String riverName;
    private Double latitude;
    private Double longitude;
    private String status;
    private Instant lastSeenAt;
    private Instant createdAt;
}