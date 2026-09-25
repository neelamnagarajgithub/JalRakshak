# JalRakshak API Documentation

## Base URL

`http://localhost:9000/api/v1`

## Endpoints

### Stations
- `GET /stations` - List all stations
- `POST /stations` - Create a new station
- `GET /stations/{id}` - Get station by ID

### Measurements
- `POST /measurements` - Submit a sensor reading

### Alerts
- `GET /alerts` - List active alerts
- `GET /alerts/{id}` - Get alert by ID
- `POST /alerts/{id}/acknowledge` - Acknowledge an alert
- `POST /alerts/{id}/resolve` - Resolve an alert

### Simulator
- `POST /simulator/scenarios/{name}` - Start a demo scenario

### Health
- `GET /health` - Health check

## Request/Response Examples

See the Springdoc OpenAPI UI at `/swagger-ui.html` for interactive documentation and examples.