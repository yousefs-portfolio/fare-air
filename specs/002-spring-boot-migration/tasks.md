# Tasks: Spring Boot Migration

**Input**: Design documents from `/specs/002-spring-boot-migration/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Backend module**: `backend-spring/src/main/kotlin/com/fairair/`
- **Resources**: `backend-spring/src/main/resources/`
- **Tests**: `backend-spring/src/test/kotlin/com/fairair/`
- **Build**: `backend-spring/build.gradle.kts`
- **Root build**: `settings.gradle.kts`, `gradle/libs.versions.toml`

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Create the new backend-spring module and configure build system

- [x] T001 Add Spring Boot dependencies to gradle/libs.versions.toml (spring-boot, spring-dependency-management plugin, spring-boot-starter-webflux, spring-boot-starter-actuator, kotlinx-coroutines-reactor)
- [x] T002 Create backend-spring/ directory structure mirroring plan.md layout
- [x] T003 Create backend-spring/build.gradle.kts with Spring Boot WebFlux dependencies and kotlin-spring plugin
- [x] T004 Update settings.gradle.kts to include backend-spring module and remove backend-quarkus
- [x] T005 Create backend-spring/src/main/resources/application.yml with server, fairair, and management configuration matching current application.properties

---

## Phase 2: Foundational (Core Infrastructure)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Create backend-spring/src/main/kotlin/com/fairair/FairairApplication.kt with @SpringBootApplication main class
- [x] T007 [P] Create backend-spring/src/main/kotlin/com/fairair/config/FairairProperties.kt with @ConfigurationProperties data classes for fairair.* config
- [x] T008 [P] Create backend-spring/src/main/kotlin/com/fairair/config/JacksonConfig.kt with ObjectMapper bean (JavaTimeModule, KotlinModule, ISO-8601 dates, ignore unknown properties)
- [x] T009 [P] Create backend-spring/src/main/kotlin/com/fairair/config/CorsConfig.kt implementing WebFluxConfigurer with CORS mappings matching current Quarkus config
- [x] T010 Create backend-spring/src/main/kotlin/com/fairair/config/CacheConfig.kt with Caffeine CacheManager bean (routes 24h TTL, searches 5min TTL)
- [x] T011 Create backend-spring/src/main/kotlin/com/fairair/exception/Exceptions.kt with FairairException hierarchy (NotFoundException, ValidationException, BookingException, ExternalServiceException)
- [x] T012 Create backend-spring/src/main/kotlin/com/fairair/exception/GlobalExceptionHandler.kt with @ControllerAdvice and @ExceptionHandler methods matching current error response format
- [x] T013 Verify application starts with ./gradlew :backend-spring:bootRun and check actuator/health responds

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Backend API Continuity (Priority: P1) 🎯 MVP

**Goal**: Migrate all booking and search endpoints to Spring Boot WebFlux while maintaining 100% API compatibility

**Independent Test**: Run the existing frontend application against the new Spring Boot backend and verify all booking flows complete successfully

### Implementation for User Story 1

- [x] T014 [P] [US1] Create backend-spring/src/main/kotlin/com/fairair/client/NavitaireClient.kt interface with suspend functions (searchFlights, createBooking, getBooking, getRoutes, getStations)
- [x] T015 [P] [US1] Migrate backend-spring/src/main/kotlin/com/fairair/service/FlightService.kt with @Service annotation preserving all business logic, convert to constructor injection
- [x] T016 [US1] Migrate backend-spring/src/main/kotlin/com/fairair/service/BookingService.kt with @Service annotation preserving validation logic, convert to constructor injection
- [x] T017 [US1] Create backend-spring/src/main/kotlin/com/fairair/controller/SearchController.kt with @RestController, @RequestMapping("/v1/search"), suspend POST endpoint returning identical response format
- [x] T018 [US1] Create backend-spring/src/main/kotlin/com/fairair/controller/BookingController.kt with @RestController, @RequestMapping("/v1/booking"), suspend POST and GET endpoints preserving all DTOs and error handling
- [x] T019 [US1] Verify search endpoint returns identical JSON to Quarkus implementation using curl comparison
- [x] T020 [US1] Verify booking endpoints return identical JSON to Quarkus implementation using curl comparison

**Checkpoint**: Search and booking APIs should now be fully functional and return identical responses

---

## Phase 4: User Story 2 - Configuration and Station Data (Priority: P2)

**Goal**: Migrate configuration endpoints with proper caching behavior

**Independent Test**: Call configuration endpoints and verify station lists, route maps are returned correctly with caching

### Implementation for User Story 2

- [x] T021 [P] [US2] Migrate backend-spring/src/main/kotlin/com/fairair/cache/CacheService.kt with @Service and Caffeine cache integration, preserving TTL logic
- [x] T022 [US2] Create backend-spring/src/main/kotlin/com/fairair/controller/ConfigController.kt with @RestController, @RequestMapping("/v1/config"), GET endpoints for /routes and /stations
- [x] T023 [US2] Add @Cacheable annotations to CacheService methods for routes (24h) and stations (24h) caching
- [x] T024 [US2] Verify /v1/config/routes returns identical JSON to Quarkus implementation
- [x] T025 [US2] Verify /v1/config/stations returns identical JSON to Quarkus implementation
- [x] T026 [US2] Verify cache behavior by checking second request returns cached data (check logs or timing)

**Checkpoint**: Configuration endpoints should now work with proper caching

---

## Phase 5: User Story 3 - Health Monitoring (Priority: P3)

**Goal**: Configure Spring Boot Actuator for health endpoints compatible with current paths

**Independent Test**: Call health endpoints and verify appropriate status responses for Kubernetes probes

### Implementation for User Story 3

- [x] T027 [US3] Configure Spring Boot Actuator in application.yml for health endpoint exposure with probes enabled
- [x] T028 [US3] Create backend-spring/src/main/kotlin/com/fairair/controller/HealthController.kt with @RestController to map /health/* to actuator endpoints OR configure actuator base-path
- [x] T029 [US3] Verify /health returns {"status":"UP"} or compatible format
- [x] T030 [US3] Verify /health/live returns 200 OK for liveness probe
- [x] T031 [US3] Verify /health/ready returns 200 OK for readiness probe

**Checkpoint**: Health endpoints should now work for Kubernetes/monitoring integration

---

## Phase 6: User Story 4 - Provider Toggle Functionality (Priority: P3)

**Goal**: Implement mock/real provider toggle using Spring conditional beans

**Independent Test**: Switch provider configuration and verify different data sources are used

### Implementation for User Story 4

- [x] T032 [US4] Create backend-spring/src/main/kotlin/com/fairair/client/MockNavitaireClient.kt with @Service and @ConditionalOnProperty(name=["fairair.provider"], havingValue="mock", matchIfMissing=true)
- [x] T033 [US4] Add delay simulation to MockNavitaireClient using config.mock.minDelay/maxDelay
- [x] T034 [US4] Verify mock provider is used when fairair.provider=mock (default)
- [x] T035 [US4] Create placeholder backend-spring/src/main/kotlin/com/fairair/client/RealNavitaireClient.kt with @Service and @ConditionalOnProperty(name=["fairair.provider"], havingValue="real") (implementation deferred)
- [x] T036 [US4] Verify switching fairair.provider=real activates RealNavitaireClient (will fail as expected since real impl is placeholder)

**Checkpoint**: Provider toggle should now work via configuration

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup, Quarkus removal, and validation

- [x] T037 Delete backend-quarkus/ directory entirely
- [x] T038 Remove Quarkus dependencies from gradle/libs.versions.toml (quarkus-bom, quarkus-kotlin, quarkus-rest, quarkus-rest-jackson, quarkus-arc, quarkus-config-yaml, quarkus-junit5)
- [x] T039 Remove quarkus plugin from gradle/libs.versions.toml [plugins] section
- [x] T040 Update CLAUDE.md build commands section to reference backend-spring instead of backend-quarkus
- [x] T041 Run full build ./gradlew build and verify all modules compile
- [x] T042 Run ./gradlew :backend-spring:bootRun and verify application starts within 10 seconds
- [x] T043 Run frontend (apps-kmp) against Spring Boot backend and verify full booking flow works (Note: Android SDK not available on this machine - manual verification required)
- [x] T044 Validate all quickstart.md commands work with the new backend

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-6)**: All depend on Foundational phase completion
  - US1 (P1): No dependencies on other stories
  - US2 (P2): No dependencies on other stories (can run parallel to US1)
  - US3 (P3): No dependencies on other stories (can run parallel)
  - US4 (P3): Depends on US1 client interface (T014)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - MVP core functionality
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Uses CacheService from T010
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent health setup
- **User Story 4 (P3)**: Depends on T014 (NavitaireClient interface) - Implements the interface

### Within Each User Story

- Client interfaces before service implementations
- Services before controllers
- Controllers before endpoint verification

### Parallel Opportunities

- T007, T008, T009 can run in parallel (different config files)
- T014, T015 can run in parallel (interface and service in different files)
- T021 can run in parallel with Phase 3 tasks (different service file)
- User Stories 1, 2, 3 can be worked on in parallel by different team members

---

## Parallel Example: Foundational Phase

```bash
# Launch all config tasks together:
Task: "Create FairairProperties.kt"
Task: "Create JacksonConfig.kt"
Task: "Create CorsConfig.kt"
```

## Parallel Example: User Story 1

```bash
# Launch interface and service together:
Task: "Create NavitaireClient.kt interface"
Task: "Migrate FlightService.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T013)
3. Complete Phase 3: User Story 1 (T014-T020)
4. **STOP and VALIDATE**: Test search and booking APIs independently
5. Continue with remaining stories

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test APIs → Core booking flow works (MVP!)
3. Add User Story 2 → Test config endpoints → Caching works
4. Add User Story 3 → Test health endpoints → Monitoring ready
5. Add User Story 4 → Test provider toggle → Dev workflow complete
6. Complete Polish → Remove Quarkus → Clean codebase

### Single Developer Strategy

Execute phases sequentially in order:
1. Phase 1 (Setup): ~30 min
2. Phase 2 (Foundational): ~1-2 hours
3. Phase 3 (US1): ~2-3 hours - **Checkpoint: MVP complete**
4. Phase 4 (US2): ~1 hour
5. Phase 5 (US3): ~30 min
6. Phase 6 (US4): ~1 hour
7. Phase 7 (Polish): ~1 hour

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- All service logic is PRESERVED - only annotations and DI patterns change
- Verify JSON compatibility using curl comparisons between Quarkus and Spring Boot
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
