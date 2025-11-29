# Implementation Plan: FairAir Production Ready

**Branch**: `004-fairair-production-ready` | **Date**: 2025-11-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-fairair-production-ready/spec.md`

## Summary

Transform the existing flyadeal airline booking application into FairAir with a complete rebrand (new name, teal/coral color scheme) while ensuring 100% production readiness. This includes removing all stubs, TODOs, and workarounds throughout the codebase, completing the end-to-end booking flow, and ensuring cross-platform consistency across Android, iOS, and Web (Wasm). The only mock permitted is the Navitaire API client.

## Technical Context

**Language/Version**: Kotlin 2.0.21 (K2 compiler)
**Primary Dependencies**:
- Frontend: Compose Multiplatform 1.7.1, Voyager 1.1.0, Koin 4.0.0, Ktor Client 3.0.1
- Backend: Spring Boot 3.2 WebFlux, Caffeine Cache, kotlinx-coroutines
**Storage**:
- Frontend: multiplatform-settings (local preferences)
- Backend: In-memory Caffeine cache (no persistent database)
**Testing**: JUnit 5, kotlin.test
**Target Platform**: Android, iOS, Web (Wasm), JVM Backend
**Project Type**: Mobile + Web + API (Kotlin Multiplatform)
**Performance Goals**: Initial load <3s, search results <5s, interaction feedback <100ms
**Constraints**: Offline viewing for saved bookings, RTL Arabic support, cross-platform feature parity
**Scale/Scope**: ~60 screens/components, 3 client platforms, 1 backend service

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: PASS - No constitution violations. Project follows existing patterns established in prior features.

- Library-First: Shared-contract module provides standalone DTOs/routes
- Test-First: Test coverage for services and API contracts
- Simplicity: No unnecessary abstractions, YAGNI principles followed
- Observability: Structured logging in backend

## Project Structure

### Documentation (this feature)

```text
specs/004-fairair-production-ready/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Kotlin Multiplatform Structure

shared-contract/
└── src/commonMain/kotlin/com/flyadeal/contract/
    ├── dto/           # DTOs shared between frontend/backend
    ├── routes/        # API route definitions
    └── models/        # Domain models

backend-spring/
└── src/main/kotlin/com/flyadeal/
    ├── client/        # NavitaireClient (Mock + Real implementations)
    ├── config/        # Spring configuration, CORS, caching
    ├── controller/    # REST endpoints
    ├── exception/     # Error handling
    └── service/       # Business logic

apps-kmp/
├── src/commonMain/kotlin/com/flyadeal/app/
│   ├── di/            # Koin dependency injection
│   ├── localization/  # Strings, language switching
│   ├── state/         # Booking flow state
│   ├── ui/
│   │   ├── components/velocity/  # Reusable UI components
│   │   ├── screens/             # Search, Results, Settings screens
│   │   └── theme/               # Colors, Typography, Theme
│   └── viewmodel/     # Screen view models
├── src/androidMain/   # Android-specific code
├── src/iosMain/       # iOS-specific code
└── src/wasmJsMain/    # Web/Wasm-specific code
```

**Structure Decision**: Existing KMP monorepo structure preserved. Changes focus on:
1. Renaming brand references (flyadeal → FairAir)
2. Updating color scheme (purple/yellow → teal/coral)
3. Completing stub implementations
4. Ensuring cross-platform parity

## Complexity Tracking

No constitution violations requiring justification.
