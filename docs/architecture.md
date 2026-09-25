# JalRakshak Architecture

## Overview

JalRakshak follows a microservices architecture with event-driven communication using Apache Kafka.

## Components

1. **Sensor Simulator** - Generates virtual sensor data and test scenarios
2. **API Service** (Spring Boot) - REST endpoints, validation, Kafka publishing
3. **Stream Processor** (Apache Flink) - Event-time windowing, trend calculation, rule evaluation
4. **Alert Consumer** - Persists alert events to database
5. **PostgreSQL** - Relational data storage
6. **Frontend Dashboard** (React + TypeScript) - Operator interface

## Data Flow

1. Sensor readings are submitted to the API service via REST
2. API validates, deduplicates, and publishes to Kafka topic `river.measurements.v1`
3. Stream processor consumes measurements, calculates trends, evaluates rules
4. When alert conditions are met, stream processor produces alert events to `river.alerts.v1`
5. Alert consumer persists alert events to PostgreSQL
6. Frontend displays station data and alerts via REST API calls to API service

## Communication Patterns

- Synchronous: REST API (frontend ↔ API service)
- Asynchronous: Kafka (API service ↔ Stream processor ↔ Alert consumer)

## Deployment

All services can be deployed using Docker Compose for local development.