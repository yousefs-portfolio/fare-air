# Quickstart: Spring Boot Migration

**Feature**: 002-spring-boot-migration
**Date**: 2025-11-29

## Prerequisites

- JDK 17+
- Gradle 8.x (wrapper included)
- IDE with Kotlin support (IntelliJ IDEA recommended)

## Build Commands

### Build the entire project
```bash
./gradlew build
```

### Build only the Spring Boot backend
```bash
./gradlew :backend-spring:build
```

### Run the Spring Boot backend in development mode
```bash
./gradlew :backend-spring:bootRun
```

The server starts on `http://localhost:8080`.

### Run with specific profile
```bash
./gradlew :backend-spring:bootRun --args='--spring.profiles.active=dev'
```

### Run tests
```bash
# All tests
./gradlew test

# Backend tests only
./gradlew :backend-spring:test

# Single test class
./gradlew :backend-spring:test --tests "com.flyadeal.controller.BookingControllerTest"
```

### Clean build
```bash
./gradlew clean build
```

## Configuration

### Application Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

flyadeal:
  provider: mock  # or 'real' for Navitaire integration
  cache:
    routes-ttl: 86400   # 24 hours in seconds
    search-ttl: 300     # 5 minutes in seconds
  mock:
    min-delay: 500      # milliseconds
    max-delay: 1500     # milliseconds

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP port | 8080 |
| `FLYADEAL_PROVIDER` | mock or real | mock |
| `FLYADEAL_CACHE_ROUTES_TTL` | Routes cache TTL (seconds) | 86400 |
| `FLYADEAL_CACHE_SEARCH_TTL` | Search cache TTL (seconds) | 300 |

## API Endpoints

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Get Routes
```bash
curl http://localhost:8080/v1/config/routes
```

### Get Stations
```bash
curl http://localhost:8080/v1/config/stations
```

### Search Flights
```bash
curl -X POST http://localhost:8080/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "JED",
    "destination": "RUH",
    "departureDate": "2025-12-15",
    "passengers": {
      "adults": 1,
      "children": 0,
      "infants": 0
    }
  }'
```

### Create Booking
```bash
curl -X POST http://localhost:8080/v1/booking \
  -H "Content-Type: application/json" \
  -d '{
    "searchId": "<search-id-from-search-response>",
    "flightNumber": "F3101",
    "fareFamily": "FLY",
    "passengers": [{
      "type": "ADULT",
      "title": "MR",
      "firstName": "Mohammed",
      "lastName": "Al-Rashid",
      "nationality": "SA",
      "dateOfBirth": "1990-05-15",
      "documentId": "A12345678"
    }],
    "ancillaries": [],
    "contactEmail": "test@example.com",
    "payment": {
      "cardholderName": "Mohammed Al-Rashid",
      "cardNumberLast4": "4242",
      "totalAmountMinor": 45000,
      "currency": "SAR"
    }
  }'
```

### Get Booking
```bash
curl http://localhost:8080/v1/booking/ABC123
```

## Project Structure After Migration

```
flyadeal/
├── settings.gradle.kts          # Updated: backend-spring instead of backend-quarkus
├── gradle/libs.versions.toml    # Updated: Spring Boot dependencies added
├── shared-contract/             # UNCHANGED
├── apps-kmp/                    # UNCHANGED
└── backend-spring/              # NEW (replaces backend-quarkus)
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── kotlin/com/flyadeal/
        │   │   ├── FlyadealApplication.kt
        │   │   ├── cache/
        │   │   ├── client/
        │   │   ├── config/
        │   │   ├── controller/
        │   │   ├── exception/
        │   │   └── service/
        │   └── resources/
        │       └── application.yml
        └── test/
```

## Verification Steps

After completing the migration, verify:

1. **Build succeeds**
   ```bash
   ./gradlew :backend-spring:build
   ```

2. **Application starts**
   ```bash
   ./gradlew :backend-spring:bootRun
   # Check: http://localhost:8080/actuator/health returns {"status":"UP"}
   ```

3. **API compatibility**
   ```bash
   # Compare responses with the existing Quarkus backend
   curl http://localhost:8080/v1/config/routes
   curl http://localhost:8080/v1/config/stations
   ```

4. **Frontend works**
   - Start the backend: `./gradlew :backend-spring:bootRun`
   - Run Android app: `./gradlew :apps-kmp:installDebug`
   - Verify flight search and booking flows work identically

5. **Tests pass**
   ```bash
   ./gradlew :backend-spring:test
   ```

## Troubleshooting

### Port already in use
```bash
# Find and kill process using port 8080
lsof -i :8080
kill -9 <PID>
```

### Jackson serialization issues
Ensure `JavaTimeModule` and `KotlinModule` are registered in the ObjectMapper configuration.

### CORS errors from frontend
Verify CORS configuration includes the frontend origin URLs in `CorsConfig.kt`.

### Provider not switching
Restart the application after changing `flyadeal.provider` - the conditional beans are evaluated at startup.
