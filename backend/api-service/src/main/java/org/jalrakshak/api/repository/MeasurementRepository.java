
package org.jalrakshak.api.repository;

import org.jalrakshak.api.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

    Optional<Measurement> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<Measurement> findByStationIdOrderByObservedAtDesc(UUID stationId);

    List<Measurement> findTop50ByStationIdOrderByObservedAtDesc(UUID stationId);

    List<Measurement> findTop100ByOrderByObservedAtDesc();
}