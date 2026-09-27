# JalRakshak — Real-time River Monitoring and Flood-Risk Alerting Prototype

> **Scope note**: This is a prototype using simulated data and illustrative thresholds — **not** a certified flood-warning system. Do not use it for public-safety decisions.

## Features

* Station/measurement/alert REST API with request validation (Spring Boot 3.2 / Java 21)
* Event-driven ingestion via Apache Kafka, using a stable `MeasurementEvent` DTO (never the JPA entity) on the wire
* A plain Apache Flink 1.19.1 stream-processing job: per-station threshold/spike/stale detection, a sliding event-time window for rising-level trends, malformed/late-event side outputs, and per-station+rule alert suppression
* PostgreSQL persistence with Flyway-managed migrations (no `ddl-auto=update`)
* Idempotent alert persistence: redelivery/repeat occurrences never overwrite an operator's acknowledge/resolve action
* React + TypeScript dashboard: stations, recent measurements, active alerts (with acknowledge/resolve), clearly-labelled demo scenario triggers
* Local development via Docker Compose (PostgreSQL + single-node KRaft Kafka + Kafka UI)

## Architecture

```
POST /api/v1/measurements
  -> API service (validate, persist, publish MeasurementEvent)
  -> Kafka "river.measurements.v1"
  -> Stream Processor (Apache Flink: per-station rules + sliding-window trend rule + suppression)
  -> Kafka "river.alerts.v1"
  -> API service's AlertEventConsumer (idempotent upsert-by-id persistence)
  -> React + TypeScript Dashboard (polls REST API)
```

See `docs/architecture.md` for the full data-flow diagram, topic/schema table, rule thresholds, and the malformed/late-event and suppression semantics.

## Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| API framework | Spring Boot 3.2 |
| Stream processing | Apache Flink 1.19.1 (plain job, not Spring Boot) |
| Messaging | Apache Kafka (single-node KRaft for local dev) |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Frontend | React 18 + TypeScript + Vite |
| Testing | JUnit 5, Mockito |
| Packaging | Docker Compose (infra only; app processes run on the host) |

## Local Setup

### Prerequisites

* Docker and Docker Compose
* Java 21 (JDK, not just a JRE)
* Maven 3.8+
* Node.js 18+ and npm

### Quick Start

1. Start infrastructure:
   ```bash
   cd jalrakshak/infra
   docker-compose up -d
   ```
   Wait for PostgreSQL and Kafka to report healthy (`docker-compose ps`). There is no separate Zookeeper container — Kafka runs single-node KRaft mode.

2. Build the backend modules, in dependency order (a root reactor POM at `backend/pom.xml` does this in one command):
   ```bash
   cd jalrakshak/backend
   mvn install -DskipTests
   ```
   This installs `shared` to the local repo, then builds `api-service` and `stream-processor` against it.

3. Run the API service (applies Flyway migrations on startup):
   ```bash
   cd jalrakshak/backend/api-service
   mvn spring-boot:run
   ```
   API at `http://localhost:9000`, Swagger UI at `http://localhost:9000/swagger-ui.html`.

4. Run the stream processor (separate terminal — alerts are only produced while this is running):
   ```bash
   cd jalrakshak/backend
   mvn -pl stream-processor -am package -DskipTests
   cd stream-processor
   java -jar target/stream-processor-0.0.1-SNAPSHOT.jar
   ```
   Configuration is via environment variables (see `.env.example` and `docs/architecture.md` for the full list and defaults); it runs against `localhost:9092` by default, matching the Compose file.

   **Java 21 note**: the shaded jar's manifest embeds the `--add-opens`/`--add-exports` flags Flink needs on Java 17+ (see `docs/architecture.md` "Flink and Java 21"), so plain `java -jar` should work. If you instead see an `InaccessibleObjectException` mentioning `java.util`, run `./run.sh` in this directory instead — it passes the same flags explicitly on the command line.

5. Run the frontend (separate terminal):
   ```bash
   cd jalrakshak/frontend
   npm install
   npm run dev
   ```
   Dashboard at `http://localhost:3000`. It reads `VITE_API_BASE_URL` (default `http://localhost:9000/api/v1`) — set it in a `.env` file in `frontend/` if the API runs elsewhere.

### Demo Scenarios

Once the API is running **and at least one station is registered** (see `docs/demo-script.md`, or use the dashboard's "+ Add station" form), trigger demo scenarios either from the dashboard's "Demo scenarios" panel, or directly:

```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/normal
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rising-level
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rainfall
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/spike
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/offline
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/duplicate
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/delayed
```

These generate synthetic measurements only — clearly labelled as demo data in the dashboard, not a real sensor feed.

## API Endpoints

See `docs/api.md` for full request/response examples. Summary:

* `GET/POST /api/v1/stations`, `GET /api/v1/stations/{id}`
* `POST /api/v1/measurements`, `GET /api/v1/measurements`, `GET /api/v1/measurements/station/{stationId}`
* `GET /api/v1/alerts`, `GET /api/v1/alerts/{id}`, `GET /api/v1/alerts/station/{stationId}`, `POST /api/v1/alerts/{id}/acknowledge`, `POST /api/v1/alerts/{id}/resolve`
* `POST /api/v1/simulator/scenarios/{name}`
* `GET /api/v1/health`

## Kafka Topics

| Topic | Schema | Producer → Consumer |
|---|---|---|
| `river.measurements.v1` | `MeasurementEvent` (`eventId`, `stationId`, `observedAt`, `waterLevelM`, `rainfallMmPerHour`, `flowRateM3S`, `schemaVersion`) | API service → Stream processor |
| `river.alerts.v1` | `AlertEvent` (`id`, `stationId`, `ruleCode`, `severity`, `status`, `title`, `reason`, `evidence`, `triggeredAt`, `lastOccurredAt`, `repeatCount`, ...) | Stream processor → API service |

Both are configurable via `KAFKA_TOPIC_MEASUREMENTS`/`KAFKA_TOPIC_ALERTS`. Full schema and rule/window/suppression config: `docs/architecture.md`.

## Environment Variables

See `.env.example` for the full, commented list (database, Kafka, server port, CORS origin, and every stream-processor rule/window/suppression override).

## Tests

```bash
cd jalrakshak/backend
mvn test
```

Covers: shared event (de)serialization round-tripping; the API's measurement→event mapping (never leaks the JPA entity graph) and alert-persistence idempotency (redelivery/repeat occurrence never clobbers acknowledge/resolve); the stream processor's pure rule engines (`ThresholdRules`, `SpikeRuleEngine`, `RisingLevelRuleEngine`, including boundary/warm-up cases) and the Kafka deserializer's malformed-record handling. **Note:** these tests could not actually be executed in the environment this project was last worked on in — see "Verified vs. not verified" below.

## Design Decisions

* **Event ID** is the idempotency key for measurements (duplicate `eventId` is not re-persisted or re-published).
* **Timestamps**: `observedAt` (sensor's event time, used throughout stream processing) vs. `receivedAt` (API ingestion time).
* **Alert repeat tracking**: `repeatCount` and `lastOccurredAt` track repeated occurrences of the same station+rule condition inside the suppression window, without creating duplicate alert rows or clobbering operator actions.

## Known Limitations

* No authentication/authorization.
* CORS is wide open (`allowedOriginPatterns("*")` on all endpoints) — fine for local prototyping, but must be tightened to a specific origin allow-list before this is exposed anywhere beyond localhost.
* `POST /alerts/{id}/resolve?note=` accepts a note but does not currently persist it.
* Suppression state (repeat-occurrence tracking) resets on every stream-processor restart — see `docs/architecture.md` "Kafka starting-offset strategy" for what this does and doesn't affect, and how to clean up accumulated test alerts.
* Simulator scenario generators previously used fixed, non-unique `eventId`s per scenario (e.g. `evt-rising-<stationId>-<i>`), so a second click of most scenario buttons for the same station was silently treated as a duplicate and never reached Kafka — the first click worked, every one after appeared to "start" but produced nothing. Fixed by including a per-run timestamp in every generated `eventId` (matching what "Normal readings" already did correctly).
* `generateOfflineScenario()` (the "Station offline" button) is currently a no-op by design — it relies on time simply passing without new readings for a station to eventually look stale; it does not itself do anything observable immediately.
* Every simulator scenario method silently swallows exceptions from `saveMeasurementFromDto` (`catch (Exception e) { }`), so a real failure (e.g. a bad station reference) looks identical to an expected duplicate-rejection. Worth tightening if scenario failures become hard to diagnose.
* `STATION_STALE` timer restart-safety across a Flink job restart/savepoint has not been exercised in this environment (see `docs/architecture.md`).
* Delivery is at-least-once, not exactly-once (see `docs/architecture.md` "Delivery semantics").
* No authentication on the Kafka UI or brokers (local dev only, not for shared/production environments).
* Frontend has no automated tests or a chart/trend visualization yet (the dashboard shows current values and a recent-measurements table, not a time-series chart).
* MapStruct-generated mappers needed explicit `@Mapping` annotations for nested `station.id` → `stationId` flattening (this was not automatic); confirmed and fixed in both `AlertMapper` and `MeasurementMapper`.

## Verified vs. not verified

This project was completed in an environment with **no outbound network access at all** and **no Maven or JDK compiler installed**, so none of it could be built or run there — everything was written and cross-checked by hand. It has since been run for real on the user's machine: `mvn install` and `mvn package` for the `shared`/`stream-processor` modules **succeeded**, confirming the reactor POM, dependency versions, and Kryo/Kafka/Lombok compatibility are all sound. Running the resulting jar surfaced one real runtime defect — a Java 21 module-system issue in Flink's Kryo fallback serializer — which is now fixed (see `docs/architecture.md` "Flink and Java 21").

Still not independently verified: the API service's build/run, the frontend's `npm install`/build, Docker Compose bring-up, and end-to-end behavior (a measurement actually flowing through Kafka → Flink → Kafka → API → dashboard). If you hit further runtime issues, the same pattern applies: static review catches structural problems, but only running each piece surfaces environment-specific ones like this.

## License

Educational and demonstration purposes only.
