package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.MeasurementDto;
import org.jalrakshak.api.messaging.MeasurementProducer;
import org.jalrakshak.api.mapper.MeasurementMapper;
import org.jalrakshak.api.repository.MeasurementRepository;
import org.jalrakshak.api.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StationRepository stationRepository;
    private final MeasurementMapper measurementMapper;
    private final MeasurementProducer measurementProducer;

    public MeasurementService(MeasurementRepository measurementRepository, StationRepository stationRepository, MeasurementMapper measurementMapper, MeasurementProducer measurementProducer) {
        this.measurementRepository = measurementRepository;
        this.stationRepository = stationRepository;
        this.measurementMapper = measurementMapper;
        this.measurementProducer = measurementProducer;
    }

    public Measurement saveMeasurement(Measurement measurement) {
        Measurement savedMeasurement = measurementRepository.save(measurement);
        // Send to Kafka after saving
        measurementProducer.sendMeasurement(savedMeasurement);
        return savedMeasurement;
    }

    public Measurement saveMeasurementFromDto(MeasurementDto measurementDto) {
        // Check if station exists
        Station station = stationRepository.findById(measurementDto.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // Convert DTO to entity
        Measurement measurement = measurementMapper.toEntity(measurementDto);
        measurement.setStation(station);
        measurement.setReceivedAt(Instant.now()); // Set received time

        // Check for duplicate eventId
        if (measurementRepository.existsByEventId(measurement.getEventId())) {
            return measurementRepository.findByEventId(measurement.getEventId())
                    .orElseThrow(() -> new RuntimeException("Measurement with this eventId already exists"));
        }

        Measurement savedMeasurement = measurementRepository.save(measurement);
        // Send to Kafka after saving
        measurementProducer.sendMeasurement(savedMeasurement);
        return savedMeasurement;
    }

    public Optional<Measurement> getMeasurementByEventId(String eventId) {
        return measurementRepository.findByEventId(eventId);
    }

    public boolean existsByEventId(String eventId) {
        return measurementRepository.existsByEventId(eventId);
    }
}