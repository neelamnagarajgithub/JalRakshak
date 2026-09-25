package org.jalrakshak.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementResponseDto {
    private String eventId;
    private String status;
    private String message;
}