package org.jalrakshak.api.repository;

import org.jalrakshak.api.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {
    Optional<Station> findByStationCode(String stationCode);
}