package org.jalrakshak.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "measurements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Measurement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    private Double waterLevelM;
    private Double rainfallMmPerHour;
    private Double flowRateM3S;

    @Column(nullable = false)
    private Integer schemaVersion;

    @PrePersist
    protected void onCreate() {
        receivedAt = Instant.now();
    }
}