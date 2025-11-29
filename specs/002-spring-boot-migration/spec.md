# Feature Specification: Spring Boot Migration

**Feature Branch**: `002-spring-boot-migration`
**Created**: 2025-11-29
**Status**: Draft
**Input**: User description: "switch from quarkus to spring boot kotlin completely"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Backend API Continuity (Priority: P1)

As a frontend application user, I need all existing API endpoints to continue functioning identically after the migration so that my booking experience remains uninterrupted.

**Why this priority**: This is the core value proposition - the migration must be transparent to end users. If APIs break, the entire platform becomes unusable.

**Independent Test**: Can be fully tested by running the existing frontend application against the new Spring Boot backend and verifying all booking flows complete successfully.

**Acceptance Scenarios**:

1. **Given** a user searches for flights, **When** the search request is sent to the backend, **Then** the response format and data remain identical to the current Quarkus implementation
2. **Given** a user creates a booking, **When** the booking request is processed, **Then** the booking is created and returned with the same response structure
3. **Given** a user retrieves booking details, **When** the request is made with a valid PNR, **Then** the booking details are returned in the expected format
4. **Given** the frontend makes any API call, **When** the call is processed by the new backend, **Then** the HTTP status codes, headers, and response bodies match the current behavior

---

### User Story 2 - Configuration and Station Data (Priority: P2)

As a system administrator, I need the configuration endpoints (routes, stations, currency settings) to work correctly so that the application can initialize properly.

**Why this priority**: Configuration data is required at application startup and affects all subsequent operations.

**Independent Test**: Can be tested by calling configuration endpoints and verifying station lists, route maps, and currency data are returned correctly.

**Acceptance Scenarios**:

1. **Given** the application starts, **When** configuration endpoints are called, **Then** routes and stations are returned with proper caching (24-hour TTL)
2. **Given** a request for station data, **When** the stations endpoint is called, **Then** all station information including codes, names, and locations is returned
3. **Given** cached configuration data exists, **When** the cache expires, **Then** fresh data is fetched and cached again

---

### User Story 3 - Health Monitoring (Priority: P3)

As an operations engineer, I need health check endpoints to work correctly so that I can monitor application status and integrate with orchestration systems.

**Why this priority**: Health checks are critical for production deployments but don't directly affect user functionality.

**Independent Test**: Can be tested by calling health endpoints and verifying appropriate status responses.

**Acceptance Scenarios**:

1. **Given** the application is running normally, **When** the health endpoint is called, **Then** a healthy status is returned
2. **Given** a dependency is unavailable, **When** the health endpoint is called, **Then** an appropriate degraded or unhealthy status is returned
3. **Given** Kubernetes or load balancer probes, **When** liveness and readiness endpoints are called, **Then** correct HTTP status codes are returned

---

### User Story 4 - Provider Toggle Functionality (Priority: P3)

As a developer, I need the mock/real provider toggle to continue working so that I can test with mock data or connect to real Navitaire services.

**Why this priority**: Development and testing workflows depend on this capability, but it doesn't affect production users.

**Independent Test**: Can be tested by switching the provider configuration and verifying different data sources are used.

**Acceptance Scenarios**:

1. **Given** the configuration is set to mock provider, **When** API calls are made, **Then** mock data is returned
2. **Given** the configuration is set to real provider, **When** API calls are made, **Then** real Navitaire service is called
3. **Given** a provider configuration change, **When** the application restarts, **Then** the correct provider is used

---

### Edge Cases

- What happens when the backend receives malformed JSON requests? (Should return 400 Bad Request with meaningful error message)
- How does the system handle requests during cache refresh? (Should serve stale data or wait for refresh, not fail)
- What happens when the Navitaire client times out? (Should return appropriate error response with retry guidance)
- How are concurrent requests handled during high load? (Should maintain response consistency and not corrupt state)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose identical REST API endpoints at the same paths (`/v1/config/*`, `/v1/search`, `/v1/booking/*`)
- **FR-002**: System MUST return responses in the exact same JSON format using the shared-contract DTOs
- **FR-003**: System MUST maintain the same HTTP status codes for all success and error scenarios
- **FR-004**: System MUST implement the same caching strategy (24-hour TTL for routes/stations, 5-minute TTL for search results)
- **FR-005**: System MUST support the provider toggle (`flyadeal.provider` configuration) for mock/real Navitaire client switching
- **FR-006**: System MUST handle global exceptions and return standardized error responses matching current format
- **FR-007**: System MUST expose health endpoints (`/health`, `/health/live`, `/health/ready`) for monitoring
- **FR-008**: System MUST use the same serialization configuration for date/time fields (ISO-8601 format)
- **FR-009**: System MUST maintain CORS configuration for frontend access
- **FR-010**: System MUST preserve all existing service layer business logic without modification
- **FR-011**: System MUST continue to use the `:shared-contract` module as the single source of truth for DTOs

### Key Entities

- **Controllers**: REST endpoint handlers for booking, configuration, search, and health operations
- **Services**: Business logic layer (BookingService, FlightService) - logic preserved, only DI annotations change
- **Clients**: NavitaireClient interface with Mock and Real implementations - toggle mechanism preserved
- **Cache**: CacheService using Caffeine for in-memory caching with configurable TTL
- **Configuration**: Application configuration for provider selection, cache TTLs, and CORS settings

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing API endpoints respond with identical response bodies when given the same requests (100% API compatibility)
- **SC-002**: Frontend applications (Android, iOS, Web) continue to function without any code changes
- **SC-003**: Application startup time remains within acceptable bounds (under 10 seconds for cold start)
- **SC-004**: All existing integration tests pass against the new Spring Boot backend
- **SC-005**: Health check endpoints correctly report application status for monitoring systems
- **SC-006**: Cache behavior maintains the same TTL policies and hit rates
- **SC-007**: Error responses maintain the same structure and messages for frontend error handling
- **SC-008**: Zero breaking changes to the shared-contract module

## Clarifications

### Session 2025-11-29

- Q: Should the old Quarkus module be completely removed or kept temporarily? → A: Remove Quarkus module entirely, replace with Spring Boot module
- Q: Which Spring Boot web stack should be used? → A: Spring WebFlux (fully reactive, non-blocking)

## Assumptions

- The `backend-quarkus` module will be completely removed and replaced with `backend-spring` in a single migration (no parallel module coexistence)
- The existing business logic in services (BookingService, FlightService) is correct and should be preserved exactly
- Spring Boot's default Jackson configuration can be adjusted to match Quarkus serialization behavior
- The Caffeine caching library will continue to be used (framework-agnostic)
- Configuration properties will be migrated to Spring Boot's `application.yml` or `application.properties` format
- The module will be renamed from `backend-quarkus` to `backend-spring` to reflect the framework change
- JVM 17 compatibility will be maintained
- Spring WebFlux will be used as the web stack, providing fully reactive non-blocking request handling with native Kotlin coroutines support

## Out of Scope

- Adding new API endpoints or functionality
- Modifying the shared-contract module DTOs
- Changing the frontend applications
- Database integration (not currently present)
- Authentication/authorization changes
- Performance optimization beyond maintaining current baseline
- Migrating to native compilation (GraalVM)
