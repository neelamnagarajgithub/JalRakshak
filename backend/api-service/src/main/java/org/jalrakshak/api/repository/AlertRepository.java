
package org.jalrakshak.api.repository;

import org.jalrakshak.api.domain.Alert;
import org.jalrakshak.api.domain.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByStationIdOrderByTriggeredAtDesc(UUID stationId);

    List<Alert> findByStatusOrderByTriggeredAtDesc(AlertStatus status);
}