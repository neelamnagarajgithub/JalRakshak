package org.jalrakshak.api.controller;
import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.dto.MeasurementDto;
import org.jalrakshak.api.dto.MeasurementResponseDto;
import org.jalrakshak.api.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PostMapping
    public ResponseEntity<MeasurementResponseDto> submitMeasurement(@RequestBody MeasurementDto measurementDto) {
        try {
            Measurement savedMeasurement = measurementService.saveMeasurementFromDto(measurementDto);
            MeasurementResponseDto response = new MeasurementResponseDto(
                    savedMeasurement.getEventId(),
                    "ACCEPTED",
                    "Measurement accepted for processing"
            );
            return ResponseEntity.created(URI.create("/api/v1/measurements/" + savedMeasurement.getId()))
                    .body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Station not found")) {
                return ResponseEntity.badRequest()
                        .body(new MeasurementResponseDto(null, "ERROR", "Station not found"));
            } else if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(409)
                        .body(new MeasurementResponseDto(measurementDto.getEventId(), "DUPLICATE", "Measurement with this eventId already exists"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new MeasurementResponseDto(null, "ERROR", e.getMessage()));
            }
        }
    }
}