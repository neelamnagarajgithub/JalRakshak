package org.jalrakshak.api.mapper;

import javax.annotation.processing.Generated;
import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.dto.MeasurementDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-26T04:55:16+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Red Hat, Inc.)"
)
@Component
public class MeasurementMapperImpl implements MeasurementMapper {

    @Override
    public MeasurementDto toDto(Measurement measurement) {
        if ( measurement == null ) {
            return null;
        }

        MeasurementDto.MeasurementDtoBuilder measurementDto = MeasurementDto.builder();

        measurementDto.eventId( measurement.getEventId() );
        measurementDto.observedAt( measurement.getObservedAt() );
        measurementDto.waterLevelM( measurement.getWaterLevelM() );
        measurementDto.rainfallMmPerHour( measurement.getRainfallMmPerHour() );
        measurementDto.flowRateM3S( measurement.getFlowRateM3S() );
        measurementDto.schemaVersion( measurement.getSchemaVersion() );

        return measurementDto.build();
    }

    @Override
    public Measurement toEntity(MeasurementDto measurementDto) {
        if ( measurementDto == null ) {
            return null;
        }

        Measurement.MeasurementBuilder measurement = Measurement.builder();

        measurement.eventId( measurementDto.getEventId() );
        measurement.observedAt( measurementDto.getObservedAt() );
        measurement.waterLevelM( measurementDto.getWaterLevelM() );
        measurement.rainfallMmPerHour( measurementDto.getRainfallMmPerHour() );
        measurement.flowRateM3S( measurementDto.getFlowRateM3S() );
        measurement.schemaVersion( measurementDto.getSchemaVersion() );

        return measurement.build();
    }
}
