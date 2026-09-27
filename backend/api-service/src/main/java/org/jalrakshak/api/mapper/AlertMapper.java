package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.dto.AlertDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "stationId", source = "station.id")
    AlertDto toDto(Alert alert);

    @Mapping(target = "station", ignore = true)
    Alert toEntity(AlertDto alertDto);
}