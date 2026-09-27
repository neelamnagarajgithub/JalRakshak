package org.jalrakshak.api.controller;

import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.MeasurementDto;
import org.jalrakshak.api.repository.StationRepository;
import org.jalrakshak.api.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorController {

    private final StationRepository stationRepository;
    private final MeasurementService measurementService;

    public SimulatorController(StationRepository stationRepository, MeasurementService measurementService) {
        this.stationRepository = stationRepository;
        this.measurementService = measurementService;
    }

    @PostMapping("/scenarios/{name}")
    public ResponseEntity<String> startScenario(@PathVariable("name") String name) {
        switch (name.toLowerCase()) {
            case "normal":
                generateNormalReadings();
                return ResponseEntity.ok("Normal scenario started");
            case "rising-level":
                generateRisingLevelScenario();
                return ResponseEntity.ok("Rising level scenario started");
            case "rainfall":
                generateRainfallScenario();
                return ResponseEntity.ok("Rainfall scenario started");
            case "spike":
                generateSensorSpikeScenario();
                return ResponseEntity.ok("Sensor spike scenario started");
            case "offline":
                generateOfflineScenario();
                return ResponseEntity.ok("Offline scenario started");
            case "duplicate":
                generateDuplicateScenario();
                return ResponseEntity.ok("Duplicate scenario started");
            case "delayed":
                generateDelayedScenario();
                return ResponseEntity.ok("Delayed scenario started");
            default:
                return ResponseEntity.badRequest().body("Unknown scenario: " + name);
        }
    }

    private void generateNormalReadings() {
        List<Station> stations = stationRepository.findAll();
        for (Station station : stations) {
            MeasurementDto measurement = MeasurementDto.builder()
                    .eventId("evt-normal-" + station.getId() + "-" + System.currentTimeMillis())
                    .stationId(station.getId())
                    .observedAt(Instant.now())
                    .waterLevelM(ThreadLocalRandom.current().nextDouble(1.0, 3.0))
                    .rainfallMmPerHour(ThreadLocalRandom.current().nextDouble(0.0, 2.0))
                    .flowRateM3S(ThreadLocalRandom.current().nextDouble(5.0, 15.0))
                    .schemaVersion(1)
                    .build();
            try {
                measurementService.saveMeasurementFromDto(measurement);
            } catch (Exception e) {
                // Handle duplicate or other errors
            }
        }
    }

    private void generateRisingLevelScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        double baseLevel = 2.0;
        String runId = String.valueOf(System.currentTimeMillis());

        for (int i = 0; i < 10; i++) {
            MeasurementDto measurement = MeasurementDto.builder()
                    .eventId("evt-rising-" + station.getId() + "-" + i + "-" + runId)
                    .stationId(station.getId())
                    .observedAt(Instant.now().minusSeconds((9-i)*10)) // Spread over 90 seconds
                    .waterLevelM(baseLevel + i * 0.5) // Rising by 0.5m each step
                    .rainfallMmPerHour(1.0)
                    .flowRateM3S(10.0)
                    .schemaVersion(1)
                    .build();
            try {
                measurementService.saveMeasurementFromDto(measurement);
            } catch (Exception e) {
                // Handle duplicate or other errors
            }
        }
    }

    private void generateRainfallScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        String runId = String.valueOf(System.currentTimeMillis());

        for (int i = 0; i < 5; i++) {
            MeasurementDto measurement = MeasurementDto.builder()
                    .eventId("evt-rain-" + station.getId() + "-" + i + "-" + runId)
                    .stationId(station.getId())
                    .observedAt(Instant.now().minusSeconds((4-i)*15))
                    .waterLevelM(2.5)
                    .rainfallMmPerHour(10.0 + i * 5.0) // Increasing rainfall
                    .flowRateM3S(12.0)
                    .schemaVersion(1)
                    .build();
            try {
                measurementService.saveMeasurementFromDto(measurement);
            } catch (Exception e) {
                // Handle duplicate or other errors
            }
        }
    }

    private void generateSensorSpikeScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        String runId = String.valueOf(System.currentTimeMillis());

        // Normal reading
        MeasurementDto normal = MeasurementDto.builder()
                .eventId("evt-spike-normal-" + station.getId() + "-" + runId)
                .stationId(station.getId())
                .observedAt(Instant.now())
                .waterLevelM(2.0)
                .rainfallMmPerHour(1.0)
                .flowRateM3S(10.0)
                .schemaVersion(1)
                .build();

        // Spike reading
        MeasurementDto spike = MeasurementDto.builder()
                .eventId("evt-spike-" + station.getId() + "-" + runId)
                .stationId(station.getId())
                .observedAt(Instant.now())
                .waterLevelM(5.0) // Sudden spike
                .rainfallMmPerHour(1.0)
                .flowRateM3S(10.0)
                .schemaVersion(1)
                .build();

        try {
            measurementService.saveMeasurementFromDto(normal);
            measurementService.saveMeasurementFromDto(spike);
        } catch (Exception e) {
            // Handle duplicate or other errors
        }
    }

    private void generateOfflineScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        // Mark station as offline by not updating lastSeenAt
        // In a real implementation, we'd have a background process that checks lastSeenAt
        // For now, we'll just note that if no new readings come in, the station will appear offline
    }

    private void generateDuplicateScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        String runId = String.valueOf(System.currentTimeMillis());
        String eventId = "evt-duplicate-" + station.getId() + "-" + runId;

        MeasurementDto measurement = MeasurementDto.builder()
                .eventId(eventId)
                .stationId(station.getId())
                .observedAt(Instant.now())
                .waterLevelM(2.0)
                .rainfallMmPerHour(1.0)
                .flowRateM3S(10.0)
                .schemaVersion(1)
                .build();

        try {
            // Send the same measurement twice
            measurementService.saveMeasurementFromDto(measurement);
            measurementService.saveMeasurementFromDto(measurement); // This should be detected as duplicate
        } catch (Exception e) {
            // Expected for the second attempt
        }
    }

    private void generateDelayedScenario() {
        List<Station> stations = stationRepository.findAll();
        if (stations.isEmpty()) return;

        Station station = stations.get(0);
        String runId = String.valueOf(System.currentTimeMillis());

        // Send a measurement with an old timestamp (delayed)
        MeasurementDto measurement = MeasurementDto.builder()
                .eventId("evt-delayed-" + station.getId() + "-" + runId)
                .stationId(station.getId())
                .observedAt(Instant.now().minusSeconds(300)) // 5 minutes ago
                .waterLevelM(2.2)
                .rainfallMmPerHour(1.5)
                .flowRateM3S(11.0)
                .schemaVersion(1)
                .build();

        try {
            measurementService.saveMeasurementFromDto(measurement);
        } catch (Exception e) {
            // Handle duplicate or other errors
        }
    }
}