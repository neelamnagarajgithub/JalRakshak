# JalRakshak Demo Script

## Prerequisites

- Docker and Docker Compose running
- Java 21 installed
- Maven 3.8+ installed

## Setup

1. Clone the repository
2. Start infrastructure:
   ```bash
   cd jalrakshak/infra
   docker-compose up -d
   ```
3. Wait for PostgreSQL and Kafka to be healthy

## Demo Steps

### 1. Start the API Service
```bash
cd jalrakshak/backend/api-service
mvn spring-boot:run
```

### 2. Register Test Stations
```bash
# Register station 001
curl -X POST http://localhost:9000/api/v1/stations \
  -H "Content-Type: application/json" \
  -d '{
    "stationCode": "station-001",
    "name": "North River Station",
    "riverName": "Northern River",
    "latitude": 40.7128,
    "longitude": -74.0060
  }'

# Register station 002
curl -X POST http://localhost:9000/api/v1/stations \
  -H "Content-Type: application/json" \
  -d '{
    "stationCode": "station-002",
    "name": "South River Station",
    "riverName": "Southern River",
    "latitude": 40.7000,
    "longitude": -74.0100
  }'
```

### 3. Verify Stations are Registered
```bash
curl -X GET http://localhost:9000/api/v1/stations
```

### 4. Run Demo Scenarios

#### Normal Conditions
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/normal
```

#### Rising Water Level (Trend Alert)
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rising-level
```

#### Heavy Rainfall
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/rainfall
```

#### Sensor Spike
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/spike
```

#### Sensor Goes Offline
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/offline
```

#### Duplicate Reading (Idempotency Test)
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/duplicate
```

#### Delayed Reading
```bash
curl -X POST http://localhost:9000/api/v1/simulator/scenarios/delayed
```

### 5. Check Alerts
```bash
curl -X GET http://localhost:9000/api/v1/alerts
```

### 6. Acknowledge and Resolve Alerts
```bash
# Get alert ID from previous step, then:
curl -X POST http://localhost:9000/api/v1/alerts/{alertId}/acknowledge
curl -X POST http://localhost:9000/api/v1/alerts/{alertId}/resolve
```

### 7. View API Documentation
Open browser to: http://localhost:9000/swagger-ui.html

## Expected Outcomes

- Stations show as ONLINE in the dashboard
- Measurements are received and processed
- Appropriate alerts are triggered based on scenarios
- Alert lifecycle can be managed (active → acknowledged → resolved)
- Duplicate readings are properly handled
- Delayed events are processed according to late-event policy

## Cleanup

```bash
cd jalrakshak/infra
docker-compose down
```