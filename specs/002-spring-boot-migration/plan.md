# Implementation Plan: Spring Boot Migration

**Branch**: `002-spring-boot-migration` | **Date**: 2025-11-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-spring-boot-migration/spec.md`

## Summary

Migrate the fairair BFF backend from Quarkus to Spring Boot WebFlux while maintaining 100% API compatibility. The migration involves replacing the `backend-quarkus` module entirely with a new `backend-spring` module using Spring WebFlux for reactive non-blocking request handling with native Kotlin coroutines support. All existing API contracts, response formats, error handling, and caching behavior must be preserved.

## Technical Context

**Language/Version**: Kotlin 2.0.21 with JVM 17
**Primary Dependencies**: Spring Boot 3.x, Spring WebFlux, Jackson, Caffeine, kotlinx-coroutines, kotlinx-datetime
**Storage**: N/A (in-memory mock/cache only)
**Testing**: JUnit 5, WebTestClient, kotlin-test
**Target Platform**: JVM server (Linux container deployment)
**Project Type**: Multi-module Gradle project (shared-contract + backend-spring + apps-kmp)
**Performance Goals**: Maintain current latency characteristics, under 10s cold start
**Constraints**: 100% API compatibility with existing frontend, ISO-8601 date serialization, identical error response format
**Scale/Scope**: BFF serving mobile and web clients, mock provider mode + real Navitaire client toggle

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The constitution template is not yet configured for this project. Proceeding with standard software engineering best practices:

| Gate | Status | Notes |
|------|--------|-------|
| API Backward Compatibility | ✅ Pass | All endpoints preserve paths, methods, and response formats |
| Test Coverage | ✅ Pass | Integration tests will verify API contract compliance |
| Shared Contract Usage | ✅ Pass | DTOs remain in `:shared-contract` module unchanged |
| Error Handling | ✅ Pass | Global exception handling preserves current error response structure |

## Project Structure

### Documentation (this feature)

```text
specs/002-spring-boot-migration/
├── plan.md              # This file
├── research.md          # Phase 0 output - Spring Boot patterns
├── data-model.md        # Phase 1 output - Existing model preserved
├── quickstart.md        # Phase 1 output - Build/run commands
├── contracts/           # Phase 1 output - Unchanged API contracts
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
# Current structure (Quarkus - to be removed)
backend-quarkus/
├── build.gradle.kts
└── src/main/kotlin/com/fairair/
    ├── cache/CacheService.kt
    ├── client/NavitaireClient.kt, MockNavitaireClient.kt
    ├── config/FairairConfig.kt
    ├── controller/BookingController.kt, ConfigController.kt, HealthController.kt, SearchController.kt
    ├── exception/GlobalExceptionHandler.kt
    └── service/BookingService.kt, FlightService.kt

# New structure (Spring Boot WebFlux)
backend-spring/
├── build.gradle.kts              # Spring Boot dependencies
└── src/
    ├── main/
    │   ├── kotlin/com/fairair/
    │   │   ├── FairairApplication.kt        # Spring Boot main class
    │   │   ├── cache/CacheService.kt         # Preserved logic, Spring @Bean
    │   │   ├── client/
    │   │   │   ├── NavitaireClient.kt        # Interface unchanged
    │   │   │   └── MockNavitaireClient.kt    # @ConditionalOnProperty
    │   │   ├── config/
    │   │   │   ├── FairairConfig.kt         # @ConfigurationProperties
    │   │   │   ├── CorsConfig.kt             # WebFlux CORS
    │   │   │   └── JacksonConfig.kt          # ObjectMapper customization
    │   │   ├── controller/
    │   │   │   ├── BookingController.kt      # @RestController, suspend fun
    │   │   │   ├── ConfigController.kt       # @RestController
    │   │   │   ├── HealthController.kt       # Spring Actuator integration
    │   │   │   └── SearchController.kt       # @RestController
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.kt # @ControllerAdvice
    │   │   └── service/
    │   │       ├── BookingService.kt         # @Service, logic preserved
    │   │       └── FlightService.kt          # @Service, logic preserved
    │   └── resources/
    │       └── application.yml               # Spring Boot config
    └── test/
        └── kotlin/com/fairair/
            └── controller/                   # WebTestClient tests
                ├── BookingControllerTest.kt
                ├── ConfigControllerTest.kt
                └── SearchControllerTest.kt

# Unchanged modules
shared-contract/                   # DTOs - NO CHANGES
apps-kmp/                          # Frontend - NO CHANGES
```

**Structure Decision**: Replace `backend-quarkus/` module entirely with `backend-spring/` module. Preserve package structure (`com.fairair.*`) and class names to minimize diff complexity. Update `settings.gradle.kts` to reference new module.

## Complexity Tracking

No constitution violations requiring justification. The migration is a 1:1 framework replacement without adding new abstractions or patterns.

## Annotation Mapping Reference

| Quarkus (JAX-RS/CDI) | Spring WebFlux |
|----------------------|----------------|
| `@Path` | `@RequestMapping` |
| `@GET`, `@POST` | `@GetMapping`, `@PostMapping` |
| `@PathParam` | `@PathVariable` |
| `@QueryParam` | `@RequestParam` |
| `@Consumes`, `@Produces` | `MediaType in mapping |
| `@ApplicationScoped` | `@Service`, `@Component` |
| `@Inject` | `@Autowired` or constructor injection |
| `Uni<T>` | `suspend fun` / `Mono<T>` |
| `@Provider` ExceptionMapper | `@ControllerAdvice` + `@ExceptionHandler` |
| `@ConfigProperty` | `@Value` or `@ConfigurationProperties` |

## Key Migration Decisions

1. **WebFlux with Kotlin Coroutines**: Use `suspend fun` in controllers instead of `Mono`/`Flux` for cleaner async code
2. **Constructor Injection**: Prefer constructor injection over `@Autowired` field injection
3. **Configuration**: Use `@ConfigurationProperties` with data classes for type-safe config
4. **Health Endpoints**: Use Spring Boot Actuator for `/health/*` endpoints
5. **CORS**: Configure via `WebFluxConfigurer` bean
6. **Jackson**: Configure `ObjectMapper` to match Quarkus serialization (ISO-8601 dates, ignore unknown properties)
