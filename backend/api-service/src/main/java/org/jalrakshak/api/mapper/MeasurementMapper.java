package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.dto.MeasurementDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MeasurementMapper {
    MeasurementMapper INSTANCE = Mappers.getMapper(MeasurementMapper.class);

    MeasurementDto toDto(Measurement measurement);
    Measurement toEntity(MeasurementDto measurementDto);
}