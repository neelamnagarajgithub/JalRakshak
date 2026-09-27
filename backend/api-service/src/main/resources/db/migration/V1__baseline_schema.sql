-- Baseline schema for JalRakshak, matching the JPA entity mappings that
-- previously relied on spring.jpa.hibernate.ddl-auto=update.
--
-- Assumption/decision: the project's documentation (README, architecture.md)
-- names Flyway as the migration tool, but no migrations existed and the API
-- was actually relying on Hibernate's ddl-auto=update. This migration
-- establishes that as an explicit, versioned baseline (spring.flyway.
-- baseline-on-migrate=true lets it apply cleanly to a fresh database, and
-- "flyway baseline" can be used to adopt it against a database that already
-- has these tables from the old ddl-auto=update behavior).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE stations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_code   VARCHAR(255) NOT NULL UNIQUE,
    name           VARCHAR(255) NOT NULL,
    river_name     VARCHAR(255),
    latitude       DOUBLE PRECISION,
    longitude      DOUBLE PRECISION,
    status         VARCHAR(32) NOT NULL DEFAULT 'ONLINE',
    last_seen_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ
);

CREATE TABLE measurements (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id              VARCHAR(255) NOT NULL UNIQUE,
    station_id            UUID NOT NULL REFERENCES stations(id),
    observed_at           TIMESTAMPTZ NOT NULL,
    received_at           TIMESTAMPTZ NOT NULL,
    water_level_m         DOUBLE PRECISION,
    rainfall_mm_per_hour  DOUBLE PRECISION,
    flow_rate_m3s         DOUBLE PRECISION,
    schema_version        INTEGER NOT NULL
);

CREATE INDEX idx_measurements_station_observed_at
    ON measurements (station_id, observed_at DESC);

CREATE TABLE alerts (
    id               UUID PRIMARY KEY,
    station_id       UUID NOT NULL REFERENCES stations(id),
    rule_code        VARCHAR(64) NOT NULL,
    severity         VARCHAR(32) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    reason           TEXT,
    evidence         JSONB,
    triggered_at     TIMESTAMPTZ NOT NULL,
    acknowledged_at  TIMESTAMPTZ,
    resolved_at      TIMESTAMPTZ
);

CREATE INDEX idx_alerts_status_triggered_at ON alerts (status, triggered_at DESC);
CREATE INDEX idx_alerts_station_triggered_at ON alerts (station_id, triggered_at DESC);
