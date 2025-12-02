# Quickstart: Airline Booking Platform

**Branch**: `001-airline-booking-platform`
**Date**: 2025-11-25

---

## Prerequisites

### Required Software

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 17+ | Kotlin/JVM compilation |
| Kotlin | 2.0+ | Language (K2 compiler) |
| Gradle | 8.5+ | Build system |
| Android Studio | Hedgehog+ | Android development |
| Xcode | 15+ | iOS development (macOS only) |
| Node.js | 18+ | Wasm/Web development tooling |

### Environment Setup

```bash
# Verify Java
java -version  # Should show 17+

# Verify Kotlin
kotlin -version  # Should show 2.0+

# Verify Gradle
./gradlew --version
```

---

## Project Setup

### Clone and Initialize

```bash
# Clone repository
git clone https://github.com/yousefs-portfolio/fare-air.git
cd fare-air

# Checkout feature branch
git checkout 001-airline-booking-platform

# Sync Gradle (downloads dependencies)
./gradlew build
```

### Project Structure

```
fare-air/
├── shared-contract/     # Shared DTOs and API routes
├── backend-quarkus/     # Quarkus BFF backend
├── apps-kmp/            # Compose Multiplatform frontend
└── gradle/
    └── libs.versions.toml
```

---

## Running the Application

### 1. Start Backend (Quarkus)

```bash
# Development mode with hot reload
./gradlew :backend-quarkus:quarkusDev

# Backend runs at http://localhost:8080
```

**Verify backend is running:**
```bash
curl http://localhost:8080/v1/config/routes
```

### 2. Run Android App

```bash
# Install on connected device/emulator
./gradlew :apps-kmp:installDebug

# Or run from Android Studio
# Open project > Select androidApp configuration > Run
```

### 3. Run iOS App (macOS only)

```bash
# Open Xcode workspace
open apps-kmp/iosApp/iosApp.xcworkspace

# Select simulator/device > Run
```

Or via command line:
```bash
./gradlew :apps-kmp:iosSimulatorArm64Binaries
# Then open in Xcode for running
```

### 4. Run Web App (Wasm)

```bash
# Start development server
./gradlew :apps-kmp:wasmJsBrowserDevelopmentRun

# Opens browser at http://localhost:8081
```

---

## Development Workflow

### Making Changes to Shared Contract

1. Edit files in `shared-contract/src/commonMain/kotlin/`
2. Changes automatically available to both backend and frontend
3. Rebuild affected modules:
   ```bash
   ./gradlew :shared-contract:build
   ./gradlew :backend-quarkus:quarkusDev  # Restart if needed
   ./gradlew :apps-kmp:build
   ```

### Backend Development

```bash
# Start with live reload
./gradlew :backend-quarkus:quarkusDev

# Run tests
./gradlew :backend-quarkus:test

# Build production JAR
./gradlew :backend-quarkus:build
```

**Configuration:**
- Edit `backend-quarkus/src/main/resources/application.properties`
- Toggle mock/real provider: `fairair.provider=mock`

### Frontend Development

```bash
# Build all platforms
./gradlew :apps-kmp:build

# Run specific platform
./gradlew :apps-kmp:installDebug           # Android
./gradlew :apps-kmp:wasmJsBrowserRun       # Web
```

**Hot Reload:**
- Android: Use Android Studio's Apply Changes
- Web: Automatic with `wasmJsBrowserDevelopmentRun`
- iOS: Requires rebuild

---

## Testing

### Run All Tests

```bash
./gradlew test
```

### Module-Specific Tests

```bash
# Shared contract tests
./gradlew :shared-contract:test

# Backend tests
./gradlew :backend-quarkus:test

# Frontend tests (common)
./gradlew :apps-kmp:testDebugUnitTest
```

### Run Specific Test

```bash
./gradlew :backend-quarkus:test --tests "com.fairair.backend.service.FlightServiceTest"
```

---

## API Endpoints Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/config/routes` | Get route map |
| GET | `/v1/config/stations` | Get all stations |
| POST | `/v1/search` | Search flights |
| POST | `/v1/booking` | Create booking |
| GET | `/v1/booking/{pnr}` | Retrieve booking |

**Full OpenAPI spec:** `specs/001-airline-booking-platform/contracts/openapi.yaml`

---

## Mock Data

Mock data is located in `backend-quarkus/src/main/resources/mock-data/`:

| File | Description |
|------|-------------|
| `navitaire_routes.json` | Valid origin-destination pairs |
| `navitaire_availability.json` | Flight search results |
| `navitaire_pnr.json` | Booking confirmation templates |

To modify mock data, edit these files and restart Quarkus.

---

## Configuration

### Backend (Quarkus)

```properties
# backend-quarkus/src/main/resources/application.properties

# Server
quarkus.http.port=8080

# CORS (for frontend development)
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:8081,http://localhost:3000

# Provider toggle
fairair.provider=mock

# Cache TTLs (seconds)
fairair.cache.routes-ttl=86400
fairair.cache.search-ttl=300
```

### Frontend (Ktor Client)

```kotlin
// apps-kmp/src/commonMain/kotlin/.../network/ApiClient.kt

// Development
const val API_BASE_URL = "http://localhost:8080"

// Production (replace during build)
// const val API_BASE_URL = "https://api.fairair.com"
```

---

## Troubleshooting

### Gradle Sync Fails

```bash
# Clear caches
./gradlew clean
rm -rf ~/.gradle/caches
./gradlew build
```

### Backend Won't Start

```bash
# Check port 8080 is free
lsof -i :8080

# Kill existing process
kill -9 <PID>
```

### Android Emulator Issues

```bash
# Ensure emulator is running
adb devices

# If localhost doesn't work, use 10.0.2.2 for emulator
# Update API_BASE_URL to http://10.0.2.2:8080
```

### iOS Build Fails

```bash
# Clean Xcode derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# Reinstall pods (if using CocoaPods)
cd apps-kmp/iosApp
pod deintegrate && pod install
```

### Wasm Build Fails

```bash
# Ensure Node.js 18+
node -v

# Clear browser cache or use incognito
```

---

## IDE Setup

### Android Studio / IntelliJ IDEA

1. Open project root folder
2. Wait for Gradle sync to complete
3. Install Kotlin Multiplatform plugin
4. Configure JDK 17+ in Project Structure

### Xcode

1. Open `apps-kmp/iosApp/iosApp.xcworkspace`
2. Select target device/simulator
3. Build and Run

---

## Useful Commands

```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck

# Generate dependency report
./gradlew dependencies

# Build release APK
./gradlew :apps-kmp:assembleRelease

# Build production Quarkus JAR
./gradlew :backend-quarkus:quarkusBuild
```

---

## Next Steps

1. Review `specs/001-airline-booking-platform/spec.md` for requirements
2. Review `specs/001-airline-booking-platform/data-model.md` for entity details
3. Review `specs/001-airline-booking-platform/contracts/openapi.yaml` for API spec
4. Start implementation following `specs/001-airline-booking-platform/tasks.md`
