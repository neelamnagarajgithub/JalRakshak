# JalRakshak — Real-time River Monitoring and Flood-Risk Alerting Prototype

> **Important scope note**: This is a prototype using simulated data and illustrative thresholds—not a real flood warning system. Do not use it for public-safety decisions.

## Features

* Virtual river stations and simulated measurements
* REST API for station and measurement management
* Event-driven ingestion using Apache Kafka
* Stream processing for rolling trends and event-time handling
* Relational persistence with PostgreSQL
* Sensible handling of duplicates, late events, and sensor outages
* Web dashboard with live-ish updates and historical views
* Local deployment with Docker Compose
* Automated tests and documented demo

## Architecture

```
Sensor Simulator
    ↓ (REST API)
Spring Boot API (Validation • Duplicates • Kafka Publishing)
    ↓
Apache Kafka — `river.measurements.v1`
    ↓
Stream Processor (Window summaries • Trend calculations • Rule evaluation)
    ↓
Apache Kafka — `river.alerts.v1`
    ↓
Alert Consumer + PostgreSQL
    ↓
React + TypeScript Dashboard
```

## Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| Language | Java 21 | Main backend language |
| Backend Framework | Spring Boot 3.x | REST APIs, dependency injection |
| API Docs | Springdoc OpenAPI | Interactive API documentation |
| Messaging | Apache Kafka | Durable event transport |
| Stream Processing | Apache Flink | Event-time windows, streaming calculations |
| Database | PostgreSQL | Relational storage |
| Database Access | Spring Data JPA / Hibernate | Persistence layer |
| Migrations | Flyway | Versioned schema changes |
| Frontend | React + TypeScript + Vite | Operator dashboard |
| Charts | Recharts | Visualize trends |
| Testing | JUnit 5, Mockito, Testcontainers | Unit and integration testing |
| Packaging | Docker + Docker Compose | Reproducible local environment |
| Observability | Spring Boot Actuator | Health and basic metrics |

## Local Setup

### Prerequisites

* Docker and Docker Compose
* Java 21
* Maven 3.8+

### Quick Start

1. Clone the repository
2. Start infrastructure services:
   ```bash
   cd jalrakshak/infra
   docker-compose up -d
   ```
3. Wait for services to be healthy (PostgreSQL, Kafka, Zookeeper)
4. Build and run the API service:
   ```bash
   cd jalrakshak/backend/api-service
   mvn spring-boot:run
   ```
5. The API will be available at http://localhost:9000
6. API documentation at http://localhost:9000/swagger-ui.html

### Running Demo Scenarios

Once the API is running, you can trigger demo scenarios:

```bash
# Normal readings
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/normal

# Rising water level
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rising-level

# Heavy rainfall
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rainfall

# Sensor spike
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/spike

# Sensor goes offline
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/offline

# Duplicate reading
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/duplicate

# Delayed reading
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/delayed
```

## API Endpoints

### Stations
* `GET /api/v1/stations` - List all stations
* `POST /api/v1/stations` - Register a new station
* `GET /api/v1/stations/{id}` - Get station details

### Measurements
* `POST /api/v1/measurements` - Submit a sensor reading

### Alerts
* `GET /api/v1/alerts` - List active alerts
* `GET /api/v1/alerts/{id}` - Get alert details
* `POST /api/v1/alerts/{id}/acknowledge` - Acknowledge an alert
* `POST /api/v1/alerts/{id}/resolve` - Resolve an alert

### Simulator
* `POST /api/v1/simulator/scenarios/{name}` - Start a demo scenario

### Health
* `GET /api/v1/health` - Health check

## Design Decisions

* **Event ID**: Used as idempotency key to prevent duplicate measurements
* **Timestamps**:
  * `observedAt`: When the sensor measured the condition
  * `receivedAt`: When the API received the measurement
* **Duplicate Handling**: Same event ID will not create duplicate measurement records
* **Late Events**: Processed according to configured late-event policy
* **Alert Grouping**: Repeated alerts for same station/rule/window are deduplicated

## Limitations

* This is a prototype using simulated data
* Alert thresholds are illustrative, not scientifically validated
* No authentication/authorization implemented
* No persistence of alert state changes (alert_events table)
* Stream processor not yet implemented (planned extension)

## Next Steps

1. Implement stream processing with Apache Flink
2. Add frontend dashboard (React + TypeScript)
3. Implement alert consumer service
4. Add comprehensive tests
5. Enhance simulator with more realistic scenarios
6. Add monitoring and observability (Prometheus/Grafana)

## License

This project is for educational and demonstration purposes only.