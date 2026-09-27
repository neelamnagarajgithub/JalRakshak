package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Measurement;
import org.jalrakshak.shared.MeasurementEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Explicit mapping from the JPA {@link Measurement} entity to the Kafka
 * {@link MeasurementEvent} DTO.
 *
 * <p>This exists specifically so the API never publishes the JPA entity
 * (which carries a full {@code Station} object graph and persistence-only
 * fields such as {@code id}/{@code receivedAt}) onto Kafka. Only the fields
 * the stream processor actually needs are published, keyed by
 * {@code stationId} rather than a nested station object.</p>
 */
@Mapper(componentModel = "spring")
public interface MeasurementEventMapper {

    @Mapping(target = "stationId", source = "station.id")
    MeasurementEvent toEvent(Measurement measurement);
}
