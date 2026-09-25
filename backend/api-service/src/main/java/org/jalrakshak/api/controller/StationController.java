package org.jalrakshak.api.controller;

import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.StationDto;
import org.jalrakshak.api.mapper.StationMapper;
import org.jalrakshak.api.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;
    private final StationMapper stationMapper;

    public StationController(StationService stationService, StationMapper stationMapper) {
        this.stationService = stationService;
        this.stationMapper = stationMapper;
    }

    @GetMapping
    public List<StationDto> getAllStations() {
        return stationService.getAllStations().stream()
                .map(stationMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationDto> getStationById(@PathVariable String id) {
        return stationService.getStationById(java.util.UUID.fromString(id))
                .map(station -> ResponseEntity.ok(stationMapper.toDto(station)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StationDto> createStation(@RequestBody StationDto stationDto) {
        Station station = stationMapper.toEntity(stationDto);
        Station savedStation = stationService.createStation(station);
        return ResponseEntity.ok(stationMapper.toDto(savedStation));
    }
}