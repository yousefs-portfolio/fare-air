# FairAir Copilot Instructions

## Architecture Overview

This is a Kotlin Multiplatform airline booking platform with three modules:

- **`:shared-contract`** — Pure KMP module with DTOs, API routes (`ApiRoutes.kt`), and models. Both frontend and backend import this; it's the single source of truth for API contracts.
- **`:backend-spring`** — Spring Boot WebFlux BFF using reactive coroutines. Controllers → Services → `NavitaireClient` (mock or real via `fairair.provider` config).
- **`:apps-kmp`** — Compose Multiplatform frontend (Android, iOS, Wasm). Uses Voyager for navigation and Koin for DI.

## Key Commands

```bash
./gradlew build                           # Full project build
./gradlew :backend-spring:bootRun         # Run backend (port 8080)
./gradlew :apps-kmp:wasmJsBrowserRun      # Run web app (port 8081)
./gradlew :apps-kmp:installDebug          # Install Android app
./gradlew test                            # All tests
./gradlew :backend-spring:test --tests "com.fairair.e2e.*"  # E2E tests only
./deploy.sh                               # Deploy to Cloud Run demo site
./deploy.sh backend                       # Deploy backend only
./deploy.sh frontend                      # Deploy frontend only
```

## Module Conventions

### shared-contract
- All DTOs use `@Serializable` from kotlinx.serialization
- API paths defined in `ApiRoutes` object (e.g., `ApiRoutes.Auth.LOGIN`)
- Dates: ISO-8601 strings or `kotlinx.datetime` types only

### backend-spring
- Fully reactive with suspend functions throughout (no blocking calls)
- Layering: `Controller` → `Service` → `NavitaireClient`
- Toggle mock/real backend: `fairair.provider: mock|real` in `application.yml`
- Caching: Routes/Stations = 24h TTL, Search = 5min TTL (Caffeine)
- Tests extend `E2ETestBase` for WebTestClient setup with `@ActiveProfiles("test")`

### apps-kmp
- Never put business logic in `@Composable` functions; use `ScreenModel`
- ScreenModels handle Loading/Content/Error states via `StateFlow<UiState>`
- DI: ScreenModels are `factory` scoped, `BookingFlowState` is singleton
- RTL support is critical: use `LocalLocalization.current.isRtl` and `LocalLayoutDirection`

## Patterns to Follow

**ScreenModel state pattern** (see `SearchScreenModel.kt`):
```kotlin
data class MyUiState(
    val isLoading: Boolean = false,
    val content: Data? = null,
    val error: String? = null
)
private val _uiState = MutableStateFlow(MyUiState())
val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
```

**API Client calls** (see `FairairApiClient.kt`):
```kotlin
val result = apiClient.searchFlights(request)
when (result) {
    is ApiResult.Success -> handleData(result.data)
    is ApiResult.Error -> handleError(result.toDisplayMessage())
}
```

**Shared contract usage** — Always define new DTOs in `:shared-contract`, never duplicate between modules.

## Booking Flow

The booking flow follows this sequence: **Search → Results → Passengers → Ancillaries → Payment → Confirmation**

State is managed via `BookingFlowState` (singleton) which holds data across screens:
- `searchCriteria` / `searchResult` — from search
- `selectedFlight` — from results
- `passengerInfo` — from passenger entry
- `selectedAncillaries` — from extras selection
- `bookingConfirmation` — after payment

### Ancillary Services

Ancillaries are extras added before payment (see `AncillariesScreenModel.kt`):

| Service | DTOs | Backend |
|---------|------|---------|
| Baggage | `BaggageDto.kt` (options, selection) | `SeatsService` |
| Seats | `SeatMapDto.kt` (map, selection) | `SeatsService` |
| Meals | `MealDto.kt` (options, selection) | `MealsService` |

Selection pattern — per-passenger selections stored in maps:
```kotlin
val baggageSelections: Map<String, BaggageSelection>  // passengerId → selection
val mealSelections: Map<String, String>               // passengerId → mealCode
```

## Critical Constraints

1. **K2 Compiler** — Project uses Kotlin 2.0.21 with K2. Some older patterns may not compile.
2. **WebFlux Threading** — Backend uses Netty; never block threads. Use `suspend` or reactive operators.
3. **Platform HTTP Clients** — Android uses OkHttp, iOS uses Darwin, Wasm uses Ktor-JS. Platform-specific configs are in source sets.
4. **Koin Module Functions** — `createAppModule()` and `createScreenModelModule()` are functions (not vals) due to Wasm polyfill timing.

## File Reference

| Concept | Key File(s) |
|---------|-------------|
| API Routes | `shared-contract/.../api/ApiRoutes.kt` |
| DTOs | `shared-contract/.../dto/*.kt` |
| Backend Services | `backend-spring/.../service/*.kt` |
| Mock Data Generator | `backend-spring/.../client/MockNavitaireClient.kt` |
| Frontend DI Setup | `apps-kmp/.../di/AppModule.kt` |
| ScreenModel Example | `apps-kmp/.../ui/screens/search/SearchScreenModel.kt` |
| Booking Flow State | `apps-kmp/.../state/BookingFlowState.kt` |
| Localization/RTL | `apps-kmp/.../localization/LocalizationManager.kt` |
| E2E Test Base | `backend-spring/.../E2ETestBase.kt` |
