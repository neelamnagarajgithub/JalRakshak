package org.jalrakshak.api.service;

import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.domain.StationStatus;
import org.jalrakshak.api.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Station createStation(Station station) {
        station.setStatus(StationStatus.ONLINE);
        station.setLastSeenAt(Instant.now());
        return stationRepository.save(station);
    }

    public Optional<Station> getStationById(UUID id) {
        return stationRepository.findById(id);
    }

    public Optional<Station> getStationByCode(String stationCode) {
        return stationRepository.findByStationCode(stationCode);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Station updateLastSeen(UUID stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found"));
        station.setLastSeenAt(Instant.now());
        return stationRepository.save(station);
    }
}