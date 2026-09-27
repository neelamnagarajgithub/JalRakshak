package org.jalrakshak.api.mapper;

import javax.annotation.processing.Generated;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.api.dto.StationDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-27T22:34:52+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Red Hat, Inc.)"
)
@Component
public class StationMapperImpl implements StationMapper {

    @Override
    public Station toEntity(StationDto stationDto) {
        if ( stationDto == null ) {
            return null;
        }

        Station.StationBuilder station = Station.builder();

        station.status( toStatus( stationDto.getStatus() ) );
        station.id( stationDto.getId() );
        station.stationCode( stationDto.getStationCode() );
        station.name( stationDto.getName() );
        station.riverName( stationDto.getRiverName() );
        station.latitude( stationDto.getLatitude() );
        station.longitude( stationDto.getLongitude() );
        station.lastSeenAt( stationDto.getLastSeenAt() );
        station.createdAt( stationDto.getCreatedAt() );

        return station.build();
    }

    @Override
    public StationDto toDto(Station station) {
        if ( station == null ) {
            return null;
        }

        StationDto.StationDtoBuilder stationDto = StationDto.builder();

        stationDto.status( toStatusString( station.getStatus() ) );
        stationDto.id( station.getId() );
        stationDto.stationCode( station.getStationCode() );
        stationDto.name( station.getName() );
        stationDto.riverName( station.getRiverName() );
        stationDto.latitude( station.getLatitude() );
        stationDto.longitude( station.getLongitude() );
        stationDto.lastSeenAt( station.getLastSeenAt() );
        stationDto.createdAt( station.getCreatedAt() );

        return stationDto.build();
    }
}
