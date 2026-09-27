# JalRakshak API Documentation

> Prototype API. No authentication/authorization is implemented. Thresholds and demo scenarios are for illustration only.

## Base URL

`http://localhost:9000/api/v1`

Interactive docs: `http://localhost:9000/swagger-ui.html` (OpenAPI JSON at `/api-docs`).

## Stations

### `GET /stations`
Returns all registered stations.

### `POST /stations`
Registers a new station. Body validation: `stationCode` and `name` are required (non-blank); a 400 with a field-level error map is returned otherwise.

```bash
curl -X POST http://localhost:9000/api/v1/stations \
  -H "Content-Type: application/json" \
  -d '{
    "stationCode": "station-001",
    "name": "North River Station",
    "riverName": "Northern River",
    "latitude": 40.7128,
    "longitude": -74.0060
  }'
```

### `GET /stations/{id}`
Returns one station by UUID, or 404.

## Measurements

### `POST /measurements`
Submits a sensor reading. Validates the body (`eventId`, `stationId`, `observedAt`, `waterLevelM` >= 0, `rainfallMmPerHour` >= 0, `schemaVersion` > 0 are required) and returns 400 with a field-error map on failure. On success, persists the measurement, updates the station's `lastSeenAt`, and publishes a `MeasurementEvent` to Kafka topic `river.measurements.v1` (see "Kafka event schemas" below). A repeated `eventId` is treated as a duplicate: the existing record is returned and **the event is not re-published to Kafka**.

Request:
```bash
curl -X POST http://localhost:9000/api/v1/measurements \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-002",
    "stationId": "5c1f6b3e-2222-4444-8888-abcdef123456",
    "observedAt": "2026-09-26T10:15:00Z",
    "waterLevelM": 2.5,
    "rainfallMmPerHour": 1.0,
    "flowRateM3S": 10.0,
    "schemaVersion": 1
  }'
```

Response (`201 Created`):
```json
{
  "eventId": "evt-002",
  "status": "ACCEPTED",
  "message": "Measurement accepted for processing"
}
```

Other outcomes: `400` (`{"status":"ERROR", ...}`) for an unknown station or failed validation, `409` (`{"status":"DUPLICATE", ...}`) for a repeated `eventId`. "ACCEPTED" means the reading was durably persisted to PostgreSQL and handed to the Kafka producer in the same request — it does not itself confirm the stream processor evaluated it or that an alert was (or was not) raised.

### `GET /measurements`
Returns the 100 most recent measurements across all stations, newest first. Used by the dashboard's activity feed.

### `GET /measurements/station/{stationId}`
Returns the 50 most recent measurements for one station, newest first.

## Alerts

Alerts are produced by the stream processor (see `docs/architecture.md`) and persisted here by `AlertEventConsumer`/`AlertService`. This service never invents alerts itself.

### `GET /alerts`
Returns alerts with status `ACTIVE`, newest-triggered first.

### `GET /alerts/{id}`
Returns one alert by UUID, or 404.

### `GET /alerts/station/{stationId}`
Returns all alerts (any status) for one station, newest-triggered first.

### `POST /alerts/{id}/acknowledge`
Moves an `ACTIVE` alert to `ACKNOWLEDGED` and stamps `acknowledgedAt`. 400 if the alert is not currently `ACTIVE`.

### `POST /alerts/{id}/resolve`
Moves an alert to `RESOLVED` and stamps `resolvedAt`. 400 if already `RESOLVED`. Accepts an optional `?note=` query parameter; **the note is currently not persisted** (known limitation — see main README).

Alert shape (`AlertDto`):
```json
{
  "id": "b6c1...-uuid",
  "stationId": "5c1f...-uuid",
  "ruleCode": "LEVEL_HIGH",
  "severity": "HIGH",
  "status": "ACTIVE",
  "title": "Water level high",
  "reason": "Water level 4.20m >= 4.00m threshold",
  "evidence": { "stationId": "...", "eventTimestamp": "2026-09-26T10:15:00Z", "waterLevelM": 4.2, "thresholdM": 4.0, "repeatCount": 1 },
  "triggeredAt": "2026-09-26T10:15:00Z",
  "acknowledgedAt": null,
  "resolvedAt": null,
  "lastOccurredAt": "2026-09-26T10:15:00Z",
  "repeatCount": 1
}
```

## Simulator (demo data only)

### `POST /simulator/scenarios/{name}`
Generates synthetic measurements against already-registered stations to exercise a rule path. `{name}` is one of: `normal`, `rising-level`, `rainfall`, `spike`, `offline`, `duplicate`, `delayed`. Returns a plain-text confirmation, not JSON. **This is demo tooling, not a real sensor feed** — it is clearly labelled as such in the dashboard.

## Health

### `GET /health`
```json
{ "status": "UP", "timestamp": "2026-09-26T10:15:00Z", "service": "jalrakshak-api-service" }
```
