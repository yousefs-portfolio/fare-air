# Security Hardening Research

**Feature**: 005-security-hardening
**Date**: 2025-11-30
**Purpose**: Document research findings and technology decisions for security implementation

## Research Areas

### R1: JWT Authentication for Spring WebFlux

**Question**: How to implement stateless authentication in a reactive Spring Boot application?

**Decision**: Use Spring Security WebFlux with custom JWT filter using `io.jsonwebtoken:jjwt`

**Rationale**:
- Spring Security provides comprehensive security framework with WebFlux support
- JWT is stateless, eliminating session management in reactive context
- JJWT library is widely adopted, well-maintained, and provides simple API
- WebFilter integration allows non-blocking authentication in reactive pipeline

**Alternatives Considered**:

| Alternative | Pros | Cons | Rejected Because |
|------------|------|------|------------------|
| Session-based | Simple, built-in | Requires sticky sessions, scaling issues | Doesn't work well with reactive/stateless |
| OAuth2/OIDC | Industry standard, SSO | External dependency, complexity | Overkill for current B2C use case |
| API Keys | Simple implementation | Hard to rotate, less secure | Not suitable for user auth |
| Spring Security OAuth2 Resource Server | Built-in JWT support | Requires auth server setup | Too complex for initial implementation |

**Implementation Pattern**:
```kotlin
// JwtAuthenticationFilter.kt
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange.request)
        return if (token != null && jwtTokenProvider.validateToken(token)) {
            val authentication = jwtTokenProvider.getAuthentication(token)
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } else {
            chain.filter(exchange)
        }
    }
}
```

**Configuration Requirements**:
- `JWT_SECRET`: 256-bit secret key (env variable, never committed)
- `JWT_ACCESS_EXPIRY`: 15 minutes
- `JWT_REFRESH_EXPIRY`: 7 days
- Refresh tokens stored in Caffeine cache with user ID key

---

### R2: Rate Limiting Strategy

**Question**: How to implement rate limiting that prevents abuse while not impacting legitimate users?

**Decision**: Bucket4j with Caffeine backend using token bucket algorithm

**Rationale**:
- Bucket4j is production-proven rate limiting library
- Token bucket allows burst traffic while enforcing average rate
- Caffeine provides efficient in-memory storage
- Easy migration to Redis if distributed limiting needed later

**Alternatives Considered**:

| Alternative | Pros | Cons | Rejected Because |
|------------|------|------|------------------|
| Resilience4j | Full circuit breaker suite | Redis required for distributed | Adds infrastructure complexity |
| Custom sliding window | Full control | Error-prone, maintenance burden | Not worth reimplementing |
| API Gateway | Centralized, enterprise features | Not using gateway currently | Architecture change required |
| Spring Cloud Gateway | Spring native | Separate service needed | Overkill for current setup |

**Rate Limit Tiers**:

| Endpoint Category | Limit | Bucket Size | Refill Rate |
|------------------|-------|-------------|-------------|
| Public (search, routes) | 100/min | 100 | 100/min |
| Authentication | 10/min | 10 | 10/min |
| Booking creation | 10/min | 10 | 10/min |
| Payment | 5/min | 5 | 5/min |

**Implementation Pattern**:
```kotlin
// RateLimitFilter.kt
@Component
class RateLimitFilter(
    private val rateLimitConfig: RateLimitConfig
) : WebFilter {
    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, Bucket>()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val clientIp = getClientIp(exchange)
        val bucket = buckets.get(clientIp) { createBucket() }

        return if (bucket.tryConsume(1)) {
            chain.filter(exchange)
        } else {
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.headers["Retry-After"] = "60"
            exchange.response.setComplete()
        }
    }
}
```

---

### R3: Certificate Pinning for KMP

**Question**: How to implement certificate pinning across Android, iOS, and Web platforms?

**Decision**: Platform-specific implementation using expect/actual pattern

**Rationale**:
- Ktor doesn't have universal certificate pinning
- Platform-native solutions are most reliable and secure
- Android uses OkHttp (already Ktor's engine), iOS uses NSURLSession
- Web relies on browser's certificate validation (pinning not applicable)

**Alternatives Considered**:

| Alternative | Pros | Cons | Rejected Because |
|------------|------|------|------------------|
| TrustKit | Easy iOS implementation | iOS only | Need cross-platform solution |
| Custom TrustManager | Full control | Complex, error-prone | Security-sensitive, not worth risk |
| Public key pinning | More rotation-friendly | More complex extraction | Certificate pinning sufficient |
| Network security config only | XML-based, simple | Android only, less flexible | Need iOS support too |

**Platform Implementations**:

**Android (OkHttp CertificatePinner)**:
```kotlin
// androidMain/security/CertificatePinner.kt
actual fun createPinnedHttpClient(): HttpClient {
    val certificatePinner = CertificatePinner.Builder()
        .add("api.fairair.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .add("api.fairair.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // backup
        .build()

    return HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build()
        }
    }
}
```

**iOS (NSURLSession)**:
```kotlin
// iosMain/security/CertificatePinner.kt
actual fun createPinnedHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        engine {
            handleChallenge { session, task, challenge, completionHandler ->
                // Validate server certificate against pinned certificates
                validateCertificate(challenge, pinnedCertificates)
            }
        }
    }
}
```

**Certificate Management**:
- Store 2 certificate hashes (current + backup for rotation)
- Hash format: SHA-256 of SubjectPublicKeyInfo (SPKI)
- Update procedure: Add new cert, wait for propagation, remove old cert
- Development builds: Disable pinning or use debug certificates

---

### R4: Payment Tokenization

**Question**: How to handle payment card data in compliance with PCI-DSS?

**Decision**: Remove all card handling from app; use payment provider's hosted fields when real payments implemented

**Rationale**:
- PCI-DSS compliance requires card data never touch our servers
- Payment providers (Stripe, Adyen) offer hosted field solutions
- Eliminates entire class of security risks
- Current mock implementation has no real payment processing

**Alternatives Considered**:

| Alternative | Pros | Cons | Rejected Because |
|------------|------|------|------------------|
| Server-side tokenization | Control over UX | Increases PCI scope drastically | Compliance nightmare |
| P2PE hardware | Most secure | Mobile only, hardware cost | Not applicable for web/mobile |
| Custom encryption | No third-party | Still touches card data | Not PCI compliant |
| 3DS only | Reduces fraud | Doesn't address storage | Different problem |

**Current Implementation Fix**:
1. Remove hardcoded test card from PaymentScreen.kt
2. Replace card input fields with placeholder explaining payment integration pending
3. For demo purposes, use mock payment that doesn't collect real card data

**Future Implementation (when real payments needed)**:
```kotlin
// PaymentScreen.kt - Future implementation
@Composable
fun PaymentScreen() {
    // Use Stripe's PaymentSheet or Adyen's Drop-in
    // These components handle card input in an iframe/webview
    // We only receive a one-time payment token

    StripePaymentSheet(
        onPaymentResult = { result ->
            when (result) {
                is PaymentResult.Completed -> {
                    // Send paymentIntentId to backend
                    // Backend charges using Stripe SDK
                }
                is PaymentResult.Failed -> { /* Handle error */ }
                is PaymentResult.Canceled -> { /* Handle cancel */ }
            }
        }
    )
}
```

---

### R5: Encrypted Storage for Mobile

**Question**: How to securely store sensitive data on mobile devices?

**Decision**:
- Android: EncryptedSharedPreferences (Jetpack Security)
- iOS: Keychain Services via platform interop

**Rationale**:
- Both are platform-recommended secure storage solutions
- Hardware-backed encryption on modern devices
- Android: AES-256-GCM, hardware keystore when available
- iOS: AES-256-GCM with Secure Enclave when available
- No additional dependencies needed (part of platform)

**Alternatives Considered**:

| Alternative | Pros | Cons | Rejected Because |
|------------|------|------|------------------|
| SQLCipher | Full database encryption | Overkill for key-value | Too heavy for simple storage |
| Custom AES | Full control | Key management nightmare | Security-sensitive DIY |
| Realm encryption | Built-in, easy | Adds large dependency | Not using Realm |
| Third-party KMP library | Cross-platform API | Additional attack surface | Platform solutions more secure |

**Implementation Pattern**:

**Common Interface**:
```kotlin
// commonMain/security/SecureStorage.kt
expect class SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}
```

**Android Implementation**:
```kotlin
// androidMain/security/SecureStorage.kt
actual class SecureStorage(context: Context) {
    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual fun putString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }
    // ... other methods
}
```

**iOS Implementation**:
```kotlin
// iosMain/security/SecureStorage.kt
actual class SecureStorage {
    actual fun putString(key: String, value: String) {
        val query = CFDictionaryCreateMutable(...)
        // Add kSecClass, kSecAttrAccount, kSecValueData
        SecItemAdd(query, null)
    }
    // ... other methods using Security.framework
}
```

**Data to Store Encrypted**:
- Refresh tokens
- Saved booking data (for offline access)
- User preferences with PII

**Data NOT to Store**:
- Passwords (use secure auth flow)
- Card data (never store)
- Session tokens (memory only)

---

### R6: Thread Safety - Removing runBlocking

**Question**: How to fix `runBlocking` usage in reactive Spring WebFlux code?

**Decision**: Convert all service methods to suspend functions, remove all blocking calls

**Rationale**:
- `runBlocking` on Netty event loop threads causes thread pool exhaustion
- WebFlux is designed for fully non-blocking operations
- Kotlin coroutines integrate naturally with reactive streams
- Spring WebFlux has excellent coroutine support

**Problem Analysis**:

Current code (FlightService.kt:24):
```kotlin
// PROBLEM: Blocks Netty thread!
fun searchFlights(request: SearchRequest): FlightSearchResponse {
    return runBlocking {
        // This blocks the event loop thread
        navitaireClient.search(request)
    }
}
```

**Solution**:
```kotlin
// CORRECT: Suspend function, no blocking
suspend fun searchFlights(request: SearchRequest): FlightSearchResponse {
    return navitaireClient.search(request)
}

// Controller can call directly
@GetMapping("/search")
suspend fun search(@RequestBody request: SearchRequest): FlightSearchResponse {
    return flightService.searchFlights(request)
}
```

**Parallel Operations Pattern**:
```kotlin
suspend fun searchWithAvailability(request: SearchRequest): EnrichedSearchResponse {
    return coroutineScope {
        val flightsDeferred = async { navitaireClient.search(request) }
        val availabilityDeferred = async { navitaireClient.checkAvailability(request) }

        EnrichedSearchResponse(
            flights = flightsDeferred.await(),
            availability = availabilityDeferred.await()
        )
    }
}
```

**Migration Steps**:
1. Change `fun` to `suspend fun` in service methods
2. Remove all `runBlocking { }` wrappers
3. Use `coroutineScope { }` for parallel operations
4. Controllers automatically handle suspend functions in WebFlux
5. Add timeouts using `withTimeout { }` for external calls

---

## Summary of Decisions

| Area | Decision | Key Library/Approach |
|------|----------|---------------------|
| Authentication | JWT with Spring Security WebFlux | io.jsonwebtoken:jjwt |
| Rate Limiting | Token bucket with in-memory storage | bucket4j + Caffeine |
| Certificate Pinning | Platform-specific implementations | OkHttp (Android), NSURLSession (iOS) |
| Payment Security | Remove card handling, use hosted fields | Stripe PaymentSheet / Adyen Drop-in |
| Encrypted Storage | Platform secure storage | EncryptedSharedPreferences / Keychain |
| Thread Safety | Full suspend functions | Kotlin coroutines |

## Open Questions (None)

All technical decisions have been made. No NEEDS CLARIFICATION items remain.
