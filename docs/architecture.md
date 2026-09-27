# JalRakshak Architecture

> Prototype using simulated data and illustrative thresholds. Not a certified flood-warning system; do not use for public-safety decisions.

## Components

1. **API service** (`backend/api-service`, Spring Boot 3.2 / Java 21) — REST endpoints for stations/measurements/alerts, request validation, Flyway-managed PostgreSQL persistence, Kafka publishing of measurement events, and Kafka consumption of alert events for persistence. Port 9000.
2. **Shared module** (`backend/shared`) — `MeasurementEvent` / `AlertEvent` DTOs and a shared, consistently-configured Jackson `ObjectMapper` (`EventObjectMapper`), used by both the API service and the stream processor so the two sides of Kafka never drift apart on JSON shape.
3. **Stream processor** (`backend/stream-processor`) — a **plain Apache Flink 1.19.1 job** (not Spring Boot), packaged as a shaded/fat jar and submitted with `flink run` or run directly via its `main()`. Consumes `river.measurements.v1`, evaluates alert rules with per-station keyed state and event-time windows, and publishes `river.alerts.v1`.
4. **PostgreSQL 15** — relational storage for stations, measurements, and alerts. Schema is managed entirely by Flyway migrations (`db/migration/V1__baseline_schema.sql`, `V2__alert_repeat_tracking.sql`); the application uses `ddl-auto=validate`, not `update`.
5. **Frontend dashboard** (`frontend`, React + TypeScript + Vite) — displays stations, recent measurements, and active alerts, and can acknowledge/resolve alerts and trigger demo scenarios. Port 3000 (Vite dev server).

## Data flow

```
POST /api/v1/measurements
  -> API service validates + persists Measurement, updates station.lastSeenAt
  -> publishes MeasurementEvent (stable DTO, never the JPA entity) to Kafka "river.measurements.v1", keyed by stationId
       |
       v
Stream processor (Flink)
  KafkaSource (never throws on malformed JSON -> MeasurementIngestResult.malformed(...))
    -> split: valid MeasurementEvent (main) / malformed record (side output, logged)
    -> assignTimestampsAndWatermarks(observedAt, 5min bounded-out-of-orderness)
    -> keyBy(stationId)
         +-> ThresholdSpikeStaleFunction: LEVEL_HIGH, RAIN_INTENSE, LEVEL_AND_RAIN (per-event),
         |                                 SENSOR_SPIKE (rolling-median baseline), STATION_STALE (processing-time timer)
         +-> SlidingEventTimeWindows(15min size / 5min slide) + RisingLevelWindowFunction: LEVEL_RISING_FAST
             (late events routed to a side output, logged, never trigger this rule)
    -> union(candidates)
    -> keyBy(stationId + "|" + ruleCode)
    -> AlertSuppressionFunction (assigns alert id, tracks repeatCount, 1h suppression window per station+rule)
    -> KafkaSink "river.alerts.v1" (at-least-once delivery), keyed by stationId
       |
       v
API service's AlertEventConsumer (@KafkaListener on river.alerts.v1)
  -> AlertService.persistAlertEvent: upsert by AlertEvent.id
     - first occurrence: new ACTIVE row
     - repeat occurrence / redelivery of the same id: refreshes evidence/repeatCount/lastOccurredAt,
       NEVER overwrites an operator's acknowledged/resolved status
       |
       v
Frontend (polls REST every 10s): stations, recent measurements, active alerts; acknowledge/resolve actions
```

## Kafka topics

| Topic | Producer | Consumer | Key | Payload |
|---|---|---|---|---|
| `river.measurements.v1` | API service | Stream processor | `stationId` (string) | `MeasurementEvent` JSON |
| `river.alerts.v1` | Stream processor | API service | `stationId` (string) | `AlertEvent` JSON |

Both are configurable via `KAFKA_TOPIC_MEASUREMENTS` / `KAFKA_TOPIC_ALERTS`. The Confluent Kafka image in `infra/docker-compose.yml` runs a single-node KRaft (Zookeeper-free) broker with default auto-topic-creation; topics will be created on first publish, or can be created ahead of time via the bundled Kafka UI at `http://localhost:8080`.

## Provisional demo rules and thresholds

**These are illustrative demo defaults, not validated hydrological standards.** All are overridable via environment variables read by `RuleConfig` (`backend/stream-processor/.../config/RuleConfig.java`), with fail-fast validation on startup.

| Rule | Condition | Env var(s) | Default |
|---|---|---|---|
| `LEVEL_HIGH` | water level >= threshold | `RULE_LEVEL_HIGH_THRESHOLD_M` | 4.0 m |
| `RAIN_INTENSE` | rainfall >= threshold | `RULE_RAIN_INTENSE_THRESHOLD_MM_H` | 20.0 mm/h |
| `LEVEL_AND_RAIN` | level >= L **and** rainfall >= R | `RULE_LEVEL_AND_RAIN_LEVEL_THRESHOLD_M`, `RULE_LEVEL_AND_RAIN_RAIN_THRESHOLD_MM_H` | 3.0 m, 15.0 mm/h |
| `LEVEL_RISING_FAST` | rise >= threshold within a sliding event-time window | `RULE_RISING_RATE_THRESHOLD_M`, `RULE_RISING_WINDOW_MINUTES`, `RULE_RISING_SLIDE_MINUTES` | 0.5 m over 15 min, sliding every 5 min |
| `SENSOR_SPIKE` | \|current − rolling-median baseline\| >= threshold, after warm-up | `RULE_SPIKE_THRESHOLD_M`, `RULE_SPIKE_WARMUP_READINGS`, `RULE_SPIKE_BASELINE_WINDOW_MINUTES` | 1.0 m, 3 readings, 60 min baseline |
| `STATION_STALE` | no accepted reading for the timeout | `RULE_STALE_TIMEOUT_MINUTES` | 30 min |

Additional processing config: `WATERMARK_OUT_OF_ORDERNESS_MINUTES` (default 5) and `ALERT_SUPPRESSION_WINDOW_MINUTES` (default 60, per station+rule).

All threshold comparisons are inclusive (`>=`). `observedAt` is used as event time throughout.

## Malformed and late events

- A Kafka record that is not valid JSON, does not match the `MeasurementEvent` shape, or is missing a required field (`eventId`/`stationId`/`observedAt`/`schemaVersion`) or has a negative `waterLevelM`/`rainfallMmPerHour` is **never** thrown as an exception (which would crash the whole job on one bad record) and **never** silently dropped: it is routed to a side output and logged (`MALFORMED` print sink in `StreamProcessorJob`).
- An event that arrives after the 15-minute rising-level window has already closed (per the 5-minute watermark) is routed to a separate late-data side output and logged (`LATE` print sink), rather than silently discarded or fabricated as on-time.

## Repeated-alert suppression

For a given `stationId + ruleCode`, the first candidate in a 1-hour (default) window becomes a brand-new `AlertEvent` with a fresh id and `repeatCount = 1`. A further candidate for the same key inside that window is a **repeat occurrence**: it reuses the same alert id, increments `repeatCount`, and refreshes `evidence`/`lastOccurredAt` — never a duplicate row, never silently dropped. Once a candidate's event time is outside the window, the condition is treated as re-armed: a new id is issued.

On the persistence side, `AlertService.persistAlertEvent` upserts by `AlertEvent.id`, so "repeat occurrence" and "this Kafka record was redelivered" are handled by the same idempotent code path — and an operator's `ACKNOWLEDGED`/`RESOLVED` status is never overwritten by a later repeat/redelivery.

**This only works within a single run of the stream processor.** The suppression function's per-station-and-rule state lives in Flink's in-memory keyed state; it is not restored across separate `java -jar` invocations (this job doesn't run against a persistent checkpoint/savepoint that survives a full stop-and-restart). So every time the process is restarted, its suppression memory starts empty. Combined with the Kafka starting-offset strategy below, this determines whether a restart re-derives duplicate alerts for old data or not.

### Kafka starting-offset strategy

The source uses `OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST)`, **not** a bare `earliest()`. The difference matters a lot in practice:
- `earliest()` would mean "start from the beginning of the topic on *every* start," full stop — so every restart during development replays the entire `river.measurements.v1` history. Combined with suppression state resetting on restart (previous paragraph), each replay mints brand-new alert ids for measurements already alerted on in a previous run, and the alerts table grows without bound purely from routine restarts.
- `committedOffsets(EARLIEST)` instead resumes from this consumer group's last **committed** Kafka offset on every start, and only falls back to the very beginning the first time this group id has ever run (when no offset is committed yet). Checkpointing is enabled (`env.enableCheckpointing(30_000)`) specifically so those offsets actually get committed back to Kafka as the job runs (`commit.offsets.on.checkpoint=true`) — a routine restart then resumes close to where it left off instead of reprocessing everything.

This does not eliminate every possible duplicate (a crash between the last commit and the last processed record can still replay a small window, same as any at-least-once consumer — and any candidate still in flight in the suppression window at the moment of restart will lose its "repeatCount so far" and appear as a fresh alert), but it removes the unbounded runaway growth that a bare `earliest()` causes on every dev-loop restart.

### If you already have a pile of duplicate alerts from testing with the old `earliest()` behavior

Those rows are real database rows and won't clean themselves up. To clear them out and start fresh:
```bash
docker exec -it $(docker ps -qf "name=postgres") psql -U postgres -d jalrakshak -c "TRUNCATE alerts;"
```
(adjust the container name filter if your Postgres container isn't named with "postgres"; `docker-compose ps` shows the actual name). This only clears alerts — stations and measurements are untouched. If you also want the stream processor to stop re-deriving alerts for old measurements on its next start, either also `TRUNCATE measurements;`, or reset this consumer group's committed offset to the topic's current end (so it only sees new data going forward):
```bash
docker exec -it $(docker ps -qf "name=kafka") kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group jalrakshak-stream-processor --topic river.measurements.v1 --reset-offsets --to-latest --execute
```

## Station stale detection

`STATION_STALE` uses a **processing-time** timer (deleted and re-registered on every reading), not an event-time window: if a station goes silent, no new events arrive, so no watermark ever advances for that key and an event-time window would never close. Flink's checkpointed timers are expected to survive a restart from checkpoint/savepoint, but this has not been exercised against a real failure in this environment, so it is not claimed as verified restart-safe.

## Flink and Java 21

Flink 1.19.1 is the version used, with `flink-connector-kafka:3.2.0-1.19` and `kafka-clients:3.6.1`. Flink 1.19.x has documented (if still maturing) Java 17/21 runtime support.

**Known Java 21 module-system issue and its fix**: Flink's Kryo fallback serializer (via Twitter Chill) reflects into JDK-internal collection classes whenever any type in the pipeline falls back to Kryo (this pipeline's `AlertCandidate`, which carries a `Map<String,Object>` evidence field, is one such type). On Java 17+ the module system blocks that reflection by default, which surfaces as:
```
InaccessibleObjectException: Unable to make field ... java.util.Arrays$ArrayList.a accessible:
module java.base does not "opens java.util" to unnamed module
```
`bin/flink`/`start-cluster.sh` apply the required `--add-opens`/`--add-exports` flags automatically via `flink-conf.yaml`'s `env.java.opts.all`, but this module runs as a bare `java -jar`, which does not read that file. The fix is embedded in the build: `stream-processor/pom.xml`'s shade-plugin config adds `Add-Opens`/`Add-Exports` attributes directly to the shaded jar's manifest, which the JDK's `java -jar` launcher honors automatically — no extra flags needed for `java -jar target/stream-processor-*.jar`. If that ever proves insufficient (e.g. running via `java -cp` instead of `-jar`, where manifest attributes don't apply), `stream-processor/run.sh` runs the same jar with the equivalent flags spelled out explicitly on the command line.

## Delivery semantics

The Kafka sink uses `DeliveryGuarantee.AT_LEAST_ONCE`. This is **not** an exactly-once claim: end-to-end exactly-once would additionally require a transactional/idempotent producer configuration and a source/sink combination configured for it, which is not what is configured here. At-least-once delivery combined with the suppression function's stable alert ids and the API's upsert-by-id persistence is what makes redelivery safe in practice.

## Deployment

`infra/docker-compose.yml` runs PostgreSQL, a single-node KRaft Kafka broker, and Kafka UI for local development. The API service, stream processor, and frontend are run directly on the host (see main README "Local setup") rather than containerized, so they can be rebuilt/restarted quickly during development.
