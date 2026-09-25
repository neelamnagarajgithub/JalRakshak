package org.jalrakshak.api.mapper;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.dto.AlertDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlertMapper {
    AlertDto toDto(Alert alert);
    Alert toEntity(AlertDto alertDto);
}