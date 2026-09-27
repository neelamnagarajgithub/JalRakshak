package org.jalrakshak.api.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.api.domain.Station;
import org.jalrakshak.shared.MeasurementEvent;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-27T22:34:52+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Red Hat, Inc.)"
)
@Component
public class MeasurementEventMapperImpl implements MeasurementEventMapper {

    @Override
    public MeasurementEvent toEvent(Measurement measurement) {
        if ( measurement == null ) {
            return null;
        }

        MeasurementEvent.MeasurementEventBuilder measurementEvent = MeasurementEvent.builder();

        measurementEvent.stationId( measurementStationId( measurement ) );
        measurementEvent.eventId( measurement.getEventId() );
        measurementEvent.observedAt( measurement.getObservedAt() );
        measurementEvent.waterLevelM( measurement.getWaterLevelM() );
        measurementEvent.rainfallMmPerHour( measurement.getRainfallMmPerHour() );
        measurementEvent.flowRateM3S( measurement.getFlowRateM3S() );
        measurementEvent.schemaVersion( measurement.getSchemaVersion() );

        return measurementEvent.build();
    }

    private UUID measurementStationId(Measurement measurement) {
        if ( measurement == null ) {
            return null;
        }
        Station station = measurement.getStation();
        if ( station == null ) {
            return null;
        }
        UUID id = station.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
