# Implementation Plan: Airline Booking Platform (Project Shadow)

**Branch**: `001-airline-booking-platform` | **Date**: 2025-11-25 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-airline-booking-platform/spec.md`

## Summary

Build a production-ready, full-stack airline booking platform for fairair using Kotlin Multiplatform. The system enables travelers to search flights, select fare families, enter passenger details, add ancillaries, complete mock payments, and receive booking confirmations with PNR codes. The architecture comprises a Quarkus BFF backend with Navitaire integration (mock/real), and a Compose Multiplatform frontend targeting Android, iOS, and Web (Wasm) from a single codebase with full Arabic RTL support.

## Technical Context

**Language/Version**: Kotlin 2.0+ (K2 compiler)
**Primary Dependencies**:
- Backend: Quarkus (RESTEasy Reactive, Jackson/Kotlinx.Serialization), Caffeine (caching)
- Frontend: Compose Multiplatform (Material 3), Voyager (navigation), Koin (DI), Ktor Client (networking)
- Shared: kotlinx.serialization, kotlinx.datetime

**Storage**:
- Backend: In-memory cache (Caffeine) with mock JSON data files
- Frontend: DataStore or SQLDelight for local persistence (session, search history, boarding passes)

**Testing**:
- Backend: Quarkus Test, JUnit 5
- Frontend: Compose UI tests, kotlin.test for shared module
- Integration: Backend REST endpoint tests with mock Navitaire client

**Target Platform**:
- Backend: JVM (Quarkus native optional)
- Frontend: Android (minSdk 24), iOS 15+, Web (Wasm/JS)

**Project Type**: Multi-module monorepo (Gradle) with shared contract module
**Performance Goals**:
- Search results in <3 seconds (95th percentile)
- App launch to search screen in <3 seconds
- 10,000 concurrent users without degradation

**Constraints**:
- RTL support mandatory (Arabic language)
- Offline capability for saved bookings
- Mock payment processing (no real gateway integration)
- Guest checkout flow (no mandatory authentication)

**Scale/Scope**:
- 8 screens (Search, Results, Passenger, Ancillaries, Payment, Confirmation, Settings, Saved Bookings)
- 3 modules (:shared-contract, :backend-quarkus, :apps-kmp)
- 2 languages (English, Arabic)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Shared Contract as Source of Truth | PASS | All DTOs defined in :shared-contract module, used by both frontend and backend |
| Business Logic Separation | PASS | Logic in ScreenModels (frontend) and Services (backend), not in UI/Controllers |
| Test Coverage | PASS | Unit tests for shared module, integration tests for backend, UI smoke tests planned |
| Platform Abstraction | PASS | Single codebase via Compose Multiplatform, platform-specific code isolated |
| Caching Strategy | PASS | Caffeine for backend (routes 24h, search 5min TTL), local persistence for frontend |
| Mock/Real Provider Toggle | PASS | NavitaireClient interface with MockNavitaireClient and RealNavitaireClient implementations |
| Simplicity (3 modules max) | PASS | :shared-contract, :backend-quarkus, :apps-kmp |
| Technology Constraints | PASS | Adheres to prescribed stack (Kotlin 2.0, Quarkus, Compose MP, Voyager, Koin, Ktor) |

## Project Structure

### Documentation (this feature)

```text
specs/001-airline-booking-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
│   └── openapi.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Gradle Multi-Module Monorepo Structure

├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Module definitions
├── gradle/
│   └── libs.versions.toml              # Version catalog (central dependency management)
│
├── shared-contract/                    # :shared-contract - Pure Kotlin Multiplatform
│   ├── build.gradle.kts
│   └── src/
│       └── commonMain/
│           └── kotlin/
│               └── com/fairair/shared/
│                   ├── api/
│                   │   └── ApiRoutes.kt           # API path constants
│                   ├── dtos/
│                   │   ├── FlightSearchRequest.kt
│                   │   ├── FlightResponse.kt
│                   │   ├── BookingRequest.kt
│                   │   └── BookingConfirmation.kt
│                   └── models/
│                       ├── Station.kt
│                       ├── RouteMap.kt
│                       ├── FareFamily.kt
│                       └── Passenger.kt
│
├── backend-quarkus/                    # :backend-quarkus - Quarkus JVM Backend
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   │   └── com/fairair/backend/
│       │   │       ├── api/
│       │   │       │   ├── ConfigResource.kt      # /v1/config/*
│       │   │       │   ├── SearchResource.kt      # /v1/search
│       │   │       │   └── BookingResource.kt     # /v1/booking
│       │   │       ├── service/
│       │   │       │   ├── FlightService.kt
│       │   │       │   └── BookingService.kt
│       │   │       ├── client/
│       │   │       │   ├── NavitaireClient.kt     # Interface
│       │   │       │   ├── MockNavitaireClient.kt
│       │   │       │   └── RealNavitaireClient.kt # Skeleton
│       │   │       ├── cache/
│       │   │       │   └── CacheConfig.kt
│       │   │       └── exception/
│       │   │           └── GlobalExceptionHandler.kt
│       │   └── resources/
│       │       ├── application.properties
│       │       └── mock-data/
│       │           ├── navitaire_routes.json
│       │           ├── navitaire_availability.json
│       │           └── navitaire_pnr.json
│       └── test/
│           └── kotlin/
│               └── com/fairair/backend/
│                   ├── api/
│                   │   └── ConfigResourceTest.kt
│                   └── service/
│                       └── FlightServiceTest.kt
│
└── apps-kmp/                           # :apps-kmp - Compose Multiplatform
    ├── build.gradle.kts
    └── src/
        ├── commonMain/
        │   └── kotlin/
        │       └── com/fairair/app/
        │           ├── App.kt                     # Main entry point
        │           ├── di/
        │           │   └── AppModule.kt           # Koin modules
        │           ├── theme/
        │           │   ├── FairairTheme.kt
        │           │   ├── Color.kt
        │           │   └── Typography.kt
        │           ├── navigation/
        │           │   └── AppNavigator.kt
        │           ├── screens/
        │           │   ├── search/
        │           │   │   ├── SearchScreen.kt
        │           │   │   └── SearchScreenModel.kt
        │           │   ├── results/
        │           │   │   ├── ResultsScreen.kt
        │           │   │   └── ResultsScreenModel.kt
        │           │   ├── passenger/
        │           │   │   ├── PassengerScreen.kt
        │           │   │   └── PassengerScreenModel.kt
        │           │   ├── ancillaries/
        │           │   │   ├── AncillariesScreen.kt
        │           │   │   └── AncillariesScreenModel.kt
        │           │   ├── payment/
        │           │   │   ├── PaymentScreen.kt
        │           │   │   └── PaymentScreenModel.kt
        │           │   └── confirmation/
        │           │       ├── ConfirmationScreen.kt
        │           │       └── ConfirmationScreenModel.kt
        │           ├── components/
        │           │   ├── FlightCard.kt
        │           │   ├── FareFamilySelector.kt
        │           │   ├── AirportSelector.kt
        │           │   ├── DatePicker.kt
        │           │   ├── PassengerCounter.kt
        │           │   └── SkeletonLoader.kt
        │           ├── repository/
        │           │   ├── StationRepository.kt
        │           │   ├── FlightRepository.kt
        │           │   └── BookingRepository.kt
        │           ├── network/
        │           │   └── ApiClient.kt
        │           ├── localization/
        │           │   ├── LocalizationManager.kt
        │           │   └── Strings.kt
        │           └── persistence/
        │               └── LocalStorage.kt
        ├── androidMain/
        │   └── kotlin/
        │       └── com/fairair/app/
        │           ├── MainActivity.kt
        │           └── MainApplication.kt
        ├── iosMain/
        │   └── kotlin/
        │       └── com/fairair/app/
        │           └── MainViewController.kt
        └── wasmJsMain/
            └── kotlin/
                └── com/fairair/app/
                    └── Main.kt
```

**Structure Decision**: Multi-module Gradle monorepo with three modules:
1. `:shared-contract` - Pure Kotlin Multiplatform module containing all DTOs, models, and API route constants. Source of truth for data contracts shared between frontend and backend.
2. `:backend-quarkus` - Quarkus JVM backend serving as BFF (Backend for Frontend), translating clean API to Navitaire logic with caching layer.
3. `:apps-kmp` - Compose Multiplatform frontend with shared UI code in commonMain and platform-specific entry points in androidMain, iosMain, and wasmJsMain.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. All constitution checks passed.
