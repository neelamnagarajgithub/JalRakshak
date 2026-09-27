package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.MeasurementDto;
import org.jalrakshak.api.mapper.MeasurementEventMapper;
import org.jalrakshak.api.mapper.MeasurementMapper;
import org.jalrakshak.api.messaging.MeasurementProducer;
import org.jalrakshak.api.repository.MeasurementRepository;
import org.jalrakshak.api.repository.StationRepository;
import org.jalrakshak.shared.MeasurementEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accepts measurement submissions, persists them, and publishes a
 * {@link MeasurementEvent} to Kafka for the stream processor.
 *
 * <p>Idempotency: the HTTP contract and eventId-based duplicate detection
 * are unchanged from the original implementation. A duplicate submission
 * (same eventId) short-circuits before publishing to Kafka again, so a
 * client retry does not re-trigger stream processing for the same reading.</p>
 */
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StationRepository stationRepository;
    private final MeasurementMapper measurementMapper;
    private final MeasurementEventMapper measurementEventMapper;
    private final MeasurementProducer measurementProducer;

    public MeasurementService(MeasurementRepository measurementRepository,
                               StationRepository stationRepository,
                               MeasurementMapper measurementMapper,
                               MeasurementEventMapper measurementEventMapper,
                               MeasurementProducer measurementProducer) {
        this.measurementRepository = measurementRepository;
        this.stationRepository = stationRepository;
        this.measurementMapper = measurementMapper;
        this.measurementEventMapper = measurementEventMapper;
        this.measurementProducer = measurementProducer;
    }

    public Measurement saveMeasurementFromDto(MeasurementDto measurementDto) {
        // Check if station exists
        Station station = stationRepository.findById(measurementDto.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // Duplicate eventId: return the existing record without re-publishing to Kafka
        if (measurementRepository.existsByEventId(measurementDto.getEventId())) {
            return measurementRepository.findByEventId(measurementDto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Measurement with this eventId already exists"));
        }

        // Convert DTO to entity
        Measurement measurement = measurementMapper.toEntity(measurementDto);
        measurement.setStation(station);
        measurement.setReceivedAt(Instant.now());

        Measurement savedMeasurement = measurementRepository.save(measurement);

        station.setLastSeenAt(savedMeasurement.getObservedAt());
        stationRepository.save(station);

        // Publish the shared event DTO to Kafka -- never the JPA entity.
        MeasurementEvent event = measurementEventMapper.toEvent(savedMeasurement);
        measurementProducer.sendMeasurement(event);

        return savedMeasurement;
    }

    public Optional<Measurement> getMeasurementByEventId(String eventId) {
        return measurementRepository.findByEventId(eventId);
    }

    public boolean existsByEventId(String eventId) {
        return measurementRepository.existsByEventId(eventId);
    }

    /** Most recent measurements across all stations, newest first, capped at 100 rows. */
    public List<MeasurementDto> getRecentMeasurements() {
        return measurementRepository.findTop100ByOrderByObservedAtDesc().stream()
                .map(measurementMapper::toDto)
                .toList();
    }

    /** Most recent measurements for one station, newest first, capped at 50 rows. */
    public List<MeasurementDto> getRecentMeasurementsForStation(UUID stationId) {
        return measurementRepository.findTop50ByStationIdOrderByObservedAtDesc(stationId).stream()
                .map(measurementMapper::toDto)
                .toList();
    }
}
