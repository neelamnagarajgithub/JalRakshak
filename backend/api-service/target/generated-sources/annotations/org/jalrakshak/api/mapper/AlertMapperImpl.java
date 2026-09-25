package org.jalrakshak.api.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertSeverity;
import org.jalrakshak.api.domain.AlertStatus;
import org.jalrakshak.api.dto.AlertDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-26T04:55:15+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Red Hat, Inc.)"
)
@Component
public class AlertMapperImpl implements AlertMapper {

    @Override
    public AlertDto toDto(Alert alert) {
        if ( alert == null ) {
            return null;
        }

        AlertDto.AlertDtoBuilder alertDto = AlertDto.builder();

        alertDto.id( alert.getId() );
        alertDto.ruleCode( alert.getRuleCode() );
        if ( alert.getSeverity() != null ) {
            alertDto.severity( alert.getSeverity().name() );
        }
        if ( alert.getStatus() != null ) {
            alertDto.status( alert.getStatus().name() );
        }
        alertDto.title( alert.getTitle() );
        alertDto.reason( alert.getReason() );
        Map<String, Object> map = alert.getEvidence();
        if ( map != null ) {
            alertDto.evidence( new LinkedHashMap<String, Object>( map ) );
        }
        alertDto.triggeredAt( alert.getTriggeredAt() );
        alertDto.acknowledgedAt( alert.getAcknowledgedAt() );
        alertDto.resolvedAt( alert.getResolvedAt() );

        return alertDto.build();
    }

    @Override
    public Alert toEntity(AlertDto alertDto) {
        if ( alertDto == null ) {
            return null;
        }

        Alert.AlertBuilder alert = Alert.builder();

        alert.id( alertDto.getId() );
        alert.ruleCode( alertDto.getRuleCode() );
        if ( alertDto.getSeverity() != null ) {
            alert.severity( Enum.valueOf( AlertSeverity.class, alertDto.getSeverity() ) );
        }
        if ( alertDto.getStatus() != null ) {
            alert.status( Enum.valueOf( AlertStatus.class, alertDto.getStatus() ) );
        }
        alert.title( alertDto.getTitle() );
        alert.reason( alertDto.getReason() );
        Map<String, Object> map = alertDto.getEvidence();
        if ( map != null ) {
            alert.evidence( new LinkedHashMap<String, Object>( map ) );
        }
        alert.triggeredAt( alertDto.getTriggeredAt() );
        alert.acknowledgedAt( alertDto.getAcknowledgedAt() );
        alert.resolvedAt( alertDto.getResolvedAt() );

        return alert.build();
    }
}
