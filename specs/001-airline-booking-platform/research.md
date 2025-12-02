# Research: Airline Booking Platform (Project Shadow)

**Branch**: `001-airline-booking-platform`
**Date**: 2025-11-25
**Status**: Complete

## Overview

This document consolidates research findings for implementing the fairair airline booking platform. All technology choices are prescribed by the master plan; research focuses on best practices, integration patterns, and implementation guidance.

---

## 1. Kotlin Multiplatform + Compose Multiplatform

### Decision
Use Kotlin 2.0+ with K2 compiler and Compose Multiplatform for cross-platform UI targeting Android, iOS, and Web (Wasm).

### Rationale
- Single codebase reduces maintenance burden across 3 platforms
- Kotlin 2.0's K2 compiler provides faster builds and improved type inference
- Compose Multiplatform is JetBrains-backed with Material 3 support
- Native performance on each platform vs. hybrid approaches

### Best Practices
- Keep platform-specific code minimal and isolated in respective source sets (androidMain, iosMain, wasmJsMain)
- Use `expect/actual` pattern for platform-specific implementations (e.g., date pickers, local storage)
- Leverage Compose's `CompositionLocal` for theming and localization context
- Use `remember` and `derivedStateOf` for performance optimization in lists

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Flutter | Different language (Dart), less mature Wasm support |
| React Native | JavaScript runtime overhead, separate codebase from backend |
| Native per-platform | 3x development and maintenance cost |

---

## 2. Quarkus Backend Framework

### Decision
Use Quarkus with RESTEasy Reactive for the BFF (Backend for Frontend) layer.

### Rationale
- Reactive-first architecture with excellent Kotlin support
- Fast startup time (important for dev iteration)
- Native compilation option for production deployment
- Built-in support for kotlinx.serialization via extension

### Best Practices
- Use `@Path`, `@GET`, `@POST` annotations from Jakarta REST
- Return `Uni<T>` for reactive endpoints
- Configure CORS in `application.properties` for frontend development
- Use constructor injection (Quarkus default) over field injection
- Implement `ExceptionMapper<T>` for global error handling

### Configuration Patterns
```properties
# application.properties
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:8081,http://localhost:3000
fairair.provider=mock  # Toggle: mock | real
fairair.cache.routes-ttl=86400  # 24 hours in seconds
fairair.cache.search-ttl=300    # 5 minutes
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Spring Boot | Heavier footprint, slower startup |
| Ktor Server | Less mature ecosystem for enterprise patterns |
| Micronaut | Similar to Quarkus but less Kotlin adoption |

---

## 3. Voyager Navigation

### Decision
Use Voyager for multiplatform navigation with ScreenModel (ViewModel equivalent).

### Rationale
- Purpose-built for Compose Multiplatform
- Koin integration out of the box
- Type-safe navigation with screen parameters
- Built-in support for screen lifecycle management

### Best Practices
- Define screens as `data class` implementing `Screen` for type-safe arguments
- Use `ScreenModel` for business logic, never put logic in `@Composable` functions
- Implement `UiState` sealed interface pattern: `Loading`, `Content(data)`, `Error(message)`
- Use `rememberScreenModel { }` within screens

### Navigation Pattern
```kotlin
// Type-safe navigation
data class ResultsScreen(val request: FlightSearchRequest) : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ResultsScreenModel(request) }
        // ...
    }
}

// Navigate
navigator.push(ResultsScreen(searchRequest))
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Decompose | More complex setup for simple navigation needs |
| Custom navigation | Reinventing the wheel, maintenance burden |
| PreCompose | Less mature, smaller community |

---

## 4. Koin Dependency Injection

### Decision
Use Koin with annotations for dependency injection across all modules.

### Rationale
- Pure Kotlin, no code generation (faster builds)
- First-class Compose Multiplatform support
- Simple DSL for module definitions
- Runtime dependency resolution (trade-off: no compile-time safety)

### Best Practices
- Organize modules by feature: `searchModule`, `bookingModule`, `networkModule`
- Use `single<Interface> { Implementation() }` for singletons
- Use `factory { }` for per-request instances
- Use `@KoinViewModel` annotation for ScreenModels when using koin-annotations

### Module Structure
```kotlin
val appModule = module {
    single<ApiClient> { ApiClientImpl(get()) }
    single<StationRepository> { StationRepositoryImpl(get()) }
    factory { SearchScreenModel(get()) }
}
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Dagger/Hilt | Android-specific, code generation heavy |
| Kodein | Similar but less community adoption |
| Manual DI | Scalability issues, boilerplate |

---

## 5. Ktor Client Networking

### Decision
Use Ktor Client with ContentNegotiation (kotlinx.serialization) for frontend-to-backend communication.

### Rationale
- Native Kotlin, excellent multiplatform support
- Different engines per platform (OkHttp/Android, Darwin/iOS, Js/Wasm)
- First-class kotlinx.serialization integration
- Structured concurrency with coroutines

### Best Practices
- Configure a single `HttpClient` instance per app lifecycle
- Use platform-specific engines: `Android`, `Darwin`, `Js`
- Set sensible timeouts (connect: 10s, request: 30s)
- Implement retry logic for transient failures
- Use `expectSuccess = true` to throw on non-2xx responses

### Client Configuration
```kotlin
val httpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
    }
    defaultRequest {
        url(BuildConfig.API_BASE_URL)
        contentType(ContentType.Application.Json)
    }
}
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Retrofit | Android-specific, no KMP support |
| Apollo (GraphQL) | REST API prescribed, overkill for this use case |
| Custom fetch wrapper | Maintenance burden, missing features |

---

## 6. Caffeine Caching (Backend)

### Decision
Use Caffeine for in-memory caching in the Quarkus backend.

### Rationale
- High-performance, near-optimal caching
- Built-in TTL and size-based eviction
- Quarkus extension available
- No external infrastructure required (Redis overkill for this scope)

### Best Practices
- Use composite cache keys: `"search:$origin:$dest:$date"`
- Configure maximum cache size to prevent memory issues
- Log cache hits/misses for observability
- Use async loading for cache population

### Cache Configuration
```kotlin
val routesCache: Cache<String, RouteMap> = Caffeine.newBuilder()
    .expireAfterWrite(24, TimeUnit.HOURS)
    .maximumSize(1)
    .build()

val searchCache: Cache<String, FlightResponse> = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .maximumSize(1000)
    .build()
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Redis | External dependency, overkill for single-instance BFF |
| Ehcache | Heavier, more configuration |
| Guava Cache | Caffeine is the successor with better performance |

---

## 7. RTL and Localization

### Decision
Implement bidirectional layout support with runtime language switching using Compose's `CompositionLocalProvider`.

### Rationale
- fairair serves Arabic-speaking market (Saudi Arabia)
- RTL is not just text direction but complete layout mirroring
- Compose supports `LayoutDirection` natively

### Best Practices
- Use `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)` at app root
- Store language preference in local storage
- Use string resources with parameterized placeholders
- Test RTL thoroughly: navigation, scrolling, icons, padding

### Implementation Pattern
```kotlin
@Composable
fun FairairApp(language: Language) {
    val layoutDirection = if (language == Language.ARABIC)
        LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        FairairTheme {
            // App content
        }
    }
}
```

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| Device locale only | User may want Arabic on English device |
| Per-screen RTL | Inconsistent, complex state management |

---

## 8. Local Persistence (Frontend)

### Decision
Use DataStore for simple key-value storage (preferences, session) and consider SQLDelight for structured data (saved bookings).

### Rationale
- DataStore is recommended by Google for KMP preferences
- SQLDelight provides type-safe SQL for complex queries
- Both have excellent KMP support

### Best Practices
- DataStore for: language preference, recent searches, session token
- SQLDelight for: saved booking confirmations (offline access)
- Use Kotlin Flows for reactive data observation
- Encrypt sensitive data (session tokens)

### Alternatives Considered
| Alternative | Rejected Because |
|-------------|------------------|
| SharedPreferences | Android-only |
| Room | Android-only |
| Realm | Heavier, less KMP mature |

---

## 9. Credit Card Validation (Mock Payment)

### Decision
Implement client-side validation with Luhn algorithm, expiry date validation, and CVV length check. No real payment gateway integration.

### Rationale
- Spec clarification specified mock payment with real validation
- Reduces scope while providing realistic UX
- Defers PCI-DSS compliance to future integration

### Validation Rules
| Field | Validation |
|-------|------------|
| Card Number | Luhn algorithm, 13-19 digits |
| Expiry Date | MM/YY format, not in past |
| CVV | 3 digits (Visa/MC) or 4 digits (Amex) |
| Cardholder Name | Non-empty, alphabetic with spaces |

### Implementation Note
```kotlin
fun isValidCardNumber(number: String): Boolean {
    val digits = number.filter { it.isDigit() }
    if (digits.length !in 13..19) return false
    return luhnCheck(digits)
}

private fun luhnCheck(number: String): Boolean {
    var sum = 0
    var alternate = false
    for (i in number.length - 1 downTo 0) {
        var n = number[i].digitToInt()
        if (alternate) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        alternate = !alternate
    }
    return sum % 10 == 0
}
```

---

## 10. Navitaire Client Interface

### Decision
Define `NavitaireClient` interface with two implementations: `MockNavitaireClient` (reads JSON files with artificial delay) and `RealNavitaireClient` (skeleton for future external API).

### Rationale
- Decouples backend from external dependency
- Enables local development without Navitaire credentials
- Toggle via configuration property `fairair.provider`

### Interface Design
```kotlin
interface NavitaireClient {
    suspend fun getRoutes(): NavitaireRoutesResponse
    suspend fun searchFlights(request: NavitaireSearchRequest): NavitaireAvailabilityResponse
    suspend fun createBooking(request: NavitaireBookingRequest): NavitairePnrResponse
}

@ApplicationScoped
@IfBuildProperty(name = "fairair.provider", stringValue = "mock")
class MockNavitaireClient : NavitaireClient {
    // Read from resources/mock-data/*.json with 0.5-1.5s delay
}

@ApplicationScoped
@IfBuildProperty(name = "fairair.provider", stringValue = "real")
class RealNavitaireClient : NavitaireClient {
    // Feign/RestClient to external Navitaire URLs (skeleton)
}
```

---

## Summary

All technology decisions are prescribed and validated. No NEEDS CLARIFICATION items remain. The research confirms:

1. **Stack is appropriate**: Kotlin 2.0 + Compose MP + Quarkus is a solid choice for multiplatform airline booking
2. **Patterns are established**: Voyager + Koin + Ktor provide clean architecture foundation
3. **Constraints are manageable**: RTL, offline, mock payment all have clear implementation paths
4. **Risk areas identified**: Wasm maturity, iOS-specific quirks need attention during implementation

**Ready for Phase 1: Design & Contracts**
