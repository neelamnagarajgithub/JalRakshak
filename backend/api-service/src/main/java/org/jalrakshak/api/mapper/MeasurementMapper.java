package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.dto.MeasurementDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MeasurementMapper {
    MeasurementMapper INSTANCE = Mappers.getMapper(MeasurementMapper.class);

    @Mapping(target = "stationId", source = "station.id")
    MeasurementDto toDto(Measurement measurement);

    @Mapping(target = "station", ignore = true)
    Measurement toEntity(MeasurementDto measurementDto);
}