package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.domain.StationStatus;
import org.jalrakshak.api.dto.StationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(target = "status", source = "status", qualifiedByName = "toStatus")
    Station toEntity(StationDto stationDto);

    @Mapping(target = "status", source = "status", qualifiedByName = "toStatusString")
    StationDto toDto(Station station);

    @Named("toStatus")
    default StationStatus toStatus(String status) {
        return status == null || status.isBlank()
                ? StationStatus.ONLINE
                : StationStatus.valueOf(status.toUpperCase());
    }

    @Named("toStatusString")
    default String toStatusString(StationStatus status) {
        return status == null ? null : status.name();
    }
}