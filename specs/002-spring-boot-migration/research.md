# Research: Spring Boot Migration

**Feature**: 002-spring-boot-migration
**Date**: 2025-11-29

## Overview

This document captures research findings for migrating from Quarkus 3.x to Spring Boot 3.x with WebFlux, focusing on maintaining API compatibility and preserving existing business logic.

---

## 1. Spring Boot WebFlux with Kotlin Coroutines

### Decision
Use Spring WebFlux with Kotlin coroutines (`suspend fun`) for reactive endpoints instead of raw `Mono`/`Flux` types.

### Rationale
- Spring WebFlux 5.2+ has native support for Kotlin coroutines via `kotlinx-coroutines-reactor`
- Controllers can use `suspend fun` which Spring automatically converts to reactive types
- Cleaner code compared to chaining reactive operators
- Matches the current Quarkus implementation pattern which uses `runBlocking` with suspend functions
- No learning curve for Reactor operators

### Alternatives Considered
| Alternative | Why Rejected |
|-------------|--------------|
| Spring MVC (blocking) | User specifically requested WebFlux for reactive stack |
| Raw Mono/Flux | More verbose, requires Reactor knowledge, less idiomatic Kotlin |
| Ktor Server | Not Spring Boot, would require different ecosystem integration |

### Implementation Pattern
```kotlin
@RestController
@RequestMapping("/v1/booking")
class BookingController(
    private val bookingService: BookingService
) {
    @PostMapping
    suspend fun createBooking(@RequestBody request: BookingRequestDto): ResponseEntity<BookingConfirmationDto> {
        val confirmation = bookingService.createBooking(request.toModel())
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingConfirmationDto.from(confirmation))
    }
}
```

---

## 2. Dependency Injection Migration

### Decision
Use constructor injection with Kotlin primary constructors, eliminating `@Autowired` field injection.

### Rationale
- Immutable dependencies (val parameters)
- Easier testing (no reflection needed)
- Compiler-enforced dependency requirements
- Spring Boot auto-wires single-constructor classes automatically

### Alternatives Considered
| Alternative | Why Rejected |
|-------------|--------------|
| `@Autowired` field injection | Mutable state, harder to test, not idiomatic Kotlin |
| `@Autowired` setter injection | Same issues as field injection |
| Manual bean wiring | Unnecessary boilerplate |

### Migration Pattern
```kotlin
// Quarkus (before)
@ApplicationScoped
class BookingService {
    @Inject
    lateinit var navitaireClient: NavitaireClient
}

// Spring Boot (after)
@Service
class BookingService(
    private val navitaireClient: NavitaireClient
) {
    // ...
}
```

---

## 3. Configuration Properties

### Decision
Use `@ConfigurationProperties` with Kotlin data classes for type-safe configuration.

### Rationale
- Type-safe access to configuration values
- IDE auto-completion and refactoring support
- Validation via JSR-380 annotations
- Centralized configuration binding

### Alternatives Considered
| Alternative | Why Rejected |
|-------------|--------------|
| `@Value` annotations | Scattered across codebase, no type safety |
| Environment variables only | No default values, harder to document |
| Custom config loading | Unnecessary when Spring provides this |

### Implementation Pattern
```kotlin
@ConfigurationProperties(prefix = "flyadeal")
data class FlyadealProperties(
    val provider: String = "mock",
    val cache: CacheProperties = CacheProperties(),
    val mock: MockProperties = MockProperties()
)

data class CacheProperties(
    val routesTtl: Long = 86400,
    val searchTtl: Long = 300
)

data class MockProperties(
    val minDelay: Long = 500,
    val maxDelay: Long = 1500
)
```

---

## 4. Global Exception Handling

### Decision
Use `@ControllerAdvice` with `@ExceptionHandler` methods to replace JAX-RS `ExceptionMapper` providers.

### Rationale
- Standard Spring approach for centralized exception handling
- Supports multiple exception types in one class
- Can return `ResponseEntity` for full response control
- Works with reactive stack

### Alternatives Considered
| Alternative | Why Rejected |
|-------------|--------------|
| `WebExceptionHandler` | Lower-level, requires manual response building |
| Per-controller try-catch | Code duplication, inconsistent error format |
| Router function error handling | Not using functional endpoints |

### Migration Pattern
```kotlin
// Quarkus (before)
@Provider
class FlyadealExceptionMapper : ExceptionMapper<FlyadealException> {
    override fun toResponse(exception: FlyadealException): Response {
        return Response.status(exception.statusCode).entity(ErrorResponse(...)).build()
    }
}

// Spring Boot (after)
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(FlyadealException::class)
    fun handleFlyadealException(ex: FlyadealException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(ex.statusCode).body(ErrorResponse(...))
    }
}
```

---

## 5. CORS Configuration

### Decision
Configure CORS via `WebFluxConfigurer` bean instead of annotations.

### Rationale
- Centralized CORS policy
- Easier to modify without touching controllers
- Supports all origins/methods/headers configuration
- Works consistently with WebFlux

### Alternatives Considered
| Alternative | Why Rejected |
|-------------|--------------|
| `@CrossOrigin` on controllers | Scattered configuration, repetitive |
| CorsWebFilter bean | Works but WebFluxConfigurer is more idiomatic |
| Gateway-level CORS | Not applicable for standalone BFF |

### Implementation Pattern
```kotlin
@Configuration
class CorsConfig : WebFluxConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:8081", "http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("accept", "authorization", "content-type", "x-requested-with")
            .exposedHeaders("Content-Disposition")
            .maxAge(86400)
    }
}
```

---

## 6. Jackson Configuration for API Compatibility

### Decision
Configure Jackson `ObjectMapper` to match Quarkus serialization behavior exactly.

### Rationale
- Quarkus uses `quarkus.jackson.fail-on-unknown-properties=false`
- Quarkus uses `quarkus.jackson.write-dates-as-timestamps=false` (ISO-8601)
- Must maintain identical JSON output for frontend compatibility

### Key Configuration
```kotlin
@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}
```

---

## 7. Health Endpoints (Spring Boot Actuator)

### Decision
Use Spring Boot Actuator for health endpoints instead of custom implementation.

### Rationale
- Provides `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` out of the box
- Kubernetes-compatible probe endpoints
- Extensible with custom health indicators
- Reduces custom code

### Endpoint Mapping
| Current (Quarkus) | Spring Boot Actuator |
|-------------------|----------------------|
| `/health` | `/actuator/health` |
| `/health/live` | `/actuator/health/liveness` |
| `/health/ready` | `/actuator/health/readiness` |

### Configuration
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
      base-path: /  # Maps /actuator/health to /health
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
```

**Note**: Custom endpoint mapping can redirect `/health/*` to actuator paths if exact path compatibility is required.

---

## 8. Provider Toggle (Mock/Real Client)

### Decision
Use `@ConditionalOnProperty` to select between Mock and Real Navitaire client implementations.

### Rationale
- Spring native conditional bean loading
- No runtime overhead for unused implementation
- Clear configuration-driven behavior
- Same pattern as Quarkus `@IfBuildProperty` / CDI alternatives

### Implementation Pattern
```kotlin
interface NavitaireClient {
    suspend fun searchFlights(request: FlightSearchRequest): FlightSearchResponse
    suspend fun createBooking(request: BookingRequest): BookingConfirmation
    suspend fun getBooking(pnr: String): BookingConfirmation?
}

@Service
@ConditionalOnProperty(name = ["flyadeal.provider"], havingValue = "mock", matchIfMissing = true)
class MockNavitaireClient(
    private val config: FlyadealProperties
) : NavitaireClient {
    // Mock implementation
}

@Service
@ConditionalOnProperty(name = ["flyadeal.provider"], havingValue = "real")
class RealNavitaireClient : NavitaireClient {
    // Real implementation
}
```

---

## 9. Caching with Caffeine

### Decision
Continue using Caffeine cache library with Spring's `@Cacheable` integration.

### Rationale
- Caffeine is framework-agnostic, already in use
- Spring Boot has native Caffeine cache manager support
- Preserves existing TTL configuration
- No code change needed in cache logic

### Configuration
```kotlin
@Configuration
@EnableCaching
class CacheConfig(private val config: FlyadealProperties) {
    @Bean
    fun cacheManager(): CacheManager {
        val caches = mapOf(
            "routes" to buildCache(config.cache.routesTtl),
            "stations" to buildCache(config.cache.routesTtl),
            "searches" to buildCache(config.cache.searchTtl)
        )
        return SimpleCacheManager().apply {
            setCaches(caches.map { (name, cache) ->
                CaffeineCache(name, cache)
            })
        }
    }

    private fun buildCache(ttlSeconds: Long): Cache<Any, Any> {
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
            .build()
    }
}
```

---

## 10. Gradle Build Configuration

### Decision
Use Spring Boot Gradle plugin with Kotlin DSL, maintaining version catalog usage.

### Key Dependencies
```kotlin
// libs.versions.toml additions
[versions]
spring-boot = "3.2.0"

[libraries]
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux", version.ref = "spring-boot" }
spring-boot-starter-actuator = { module = "org.springframework.boot:spring-boot-starter-actuator", version.ref = "spring-boot" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test", version.ref = "spring-boot" }
kotlinx-coroutines-reactor = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor", version.ref = "kotlinx-coroutines" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version = "1.1.4" }
```

### build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    kotlin("plugin.spring") version libs.versions.kotlin
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.caffeine)
    implementation(project(":shared-contract"))

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test)
}
```

---

## Summary

All technical decisions have been resolved. The migration follows a 1:1 mapping from Quarkus to Spring Boot patterns with no architectural changes. Key considerations:

1. **Preserve business logic** - Service classes change annotations only
2. **Maintain API contracts** - Same paths, methods, response formats
3. **Use Kotlin idioms** - Coroutines, data classes, constructor injection
4. **Leverage Spring Boot conventions** - Actuator, auto-configuration, conditional beans
