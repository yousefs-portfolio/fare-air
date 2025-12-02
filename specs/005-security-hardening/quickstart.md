# Security Hardening Quickstart Guide

**Feature**: 005-security-hardening
**Date**: 2025-11-30
**Purpose**: Quick reference for implementing and testing security features

## Prerequisites

### Environment Setup

1. **JWT Secret Configuration**

   Create or update `.env` file in `backend-spring/`:
   ```bash
   # backend-spring/.env
   JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long
   JWT_ACCESS_EXPIRY_SECONDS=900
   JWT_REFRESH_EXPIRY_SECONDS=604800
   ```

   Generate a secure secret:
   ```bash
   openssl rand -base64 32
   ```

2. **CORS Configuration**

   Update `application.yml`:
   ```yaml
   # backend-spring/src/main/resources/application.yml
   fairair:
     cors:
       allowed-origins:
         - https://fairair.com
         - https://www.fairair.com
         - https://app.fairair.com
       allowed-methods: GET,POST,PUT,DELETE,OPTIONS
       max-age: 3600

   # Development override in application-dev.yml
   fairair:
     cors:
       allowed-origins:
         - http://localhost:3000
         - http://localhost:8080
   ```

3. **Rate Limiting Configuration**

   ```yaml
   # backend-spring/src/main/resources/application.yml
   fairair:
     rate-limit:
       enabled: true
       public:
         limit: 100
         window-seconds: 60
       auth:
         limit: 10
         window-seconds: 60
       payment:
         limit: 5
         window-seconds: 60
   ```

### Dependencies

Add to `gradle/libs.versions.toml`:
```toml
[versions]
jjwt = "0.12.3"
bucket4j = "8.7.0"
security-crypto = "1.1.0-alpha06"

[libraries]
jjwt-api = { module = "io.jsonwebtoken:jjwt-api", version.ref = "jjwt" }
jjwt-impl = { module = "io.jsonwebtoken:jjwt-impl", version.ref = "jjwt" }
jjwt-jackson = { module = "io.jsonwebtoken:jjwt-jackson", version.ref = "jjwt" }
bucket4j-core = { module = "com.bucket4j:bucket4j-core", version.ref = "bucket4j" }
security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }
```

Add to `backend-spring/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.bucket4j.core)
}
```

Add to `apps-kmp/build.gradle.kts`:
```kotlin
androidMain.dependencies {
    implementation(libs.security.crypto)
}
```

---

## Implementation Order

### Phase 1: Critical Fixes (Do First)

1. **Remove runBlocking from FlightService**
   ```kotlin
   // BEFORE (BAD)
   fun searchFlights(request: SearchRequest): FlightSearchResponse {
       return runBlocking { navitaireClient.search(request) }
   }

   // AFTER (GOOD)
   suspend fun searchFlights(request: SearchRequest): FlightSearchResponse {
       return navitaireClient.search(request)
   }
   ```

2. **Remove Hardcoded Test Card**
   - File: `apps-kmp/src/commonMain/kotlin/.../PaymentScreen.kt`
   - Search for: `4111111111111111` or similar test card patterns
   - Replace with empty form or mock payment flow

3. **Restrict CORS**
   - File: `backend-spring/src/main/kotlin/.../CorsConfig.kt`
   - Change: `allowedOrigins("*")` → `allowedOrigins(configuredOrigins)`

4. **Enable Android Minification**
   - File: `apps-kmp/build.gradle.kts`
   ```kotlin
   android {
       buildTypes {
           release {
               isMinifyEnabled = true
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
           }
       }
   }
   ```

5. **Make DEBUG Flag Configurable**
   - File: `apps-kmp/src/commonMain/kotlin/.../BookingConfig.kt`
   ```kotlin
   // BEFORE
   const val DEBUG = true

   // AFTER
   val DEBUG: Boolean = BuildConfig.DEBUG // or environment-based
   ```

### Phase 2: JWT Authentication

1. **Create JwtTokenProvider**
   ```kotlin
   @Component
   class JwtTokenProvider(
       @Value("\${jwt.secret}") private val secret: String,
       @Value("\${jwt.access-expiry-seconds}") private val accessExpiry: Long
   ) {
       private val key = Keys.hmacShaKeyFor(secret.toByteArray())

       fun createAccessToken(userId: String): String {
           val now = Instant.now()
           return Jwts.builder()
               .setSubject(userId)
               .setIssuedAt(Date.from(now))
               .setExpiration(Date.from(now.plusSeconds(accessExpiry)))
               .claim("type", "ACCESS")
               .signWith(key)
               .compact()
       }

       fun validateToken(token: String): Boolean {
           return try {
               Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
               true
           } catch (e: Exception) {
               false
           }
       }
   }
   ```

2. **Create JwtAuthenticationFilter**
   ```kotlin
   @Component
   class JwtAuthenticationFilter(
       private val jwtTokenProvider: JwtTokenProvider
   ) : WebFilter {
       override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
           val token = extractToken(exchange.request)
           if (token != null && jwtTokenProvider.validateToken(token)) {
               val auth = jwtTokenProvider.getAuthentication(token)
               return chain.filter(exchange)
                   .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
           }
           return chain.filter(exchange)
       }

       private fun extractToken(request: ServerHttpRequest): String? {
           val header = request.headers.getFirst("Authorization")
           return if (header?.startsWith("Bearer ") == true) header.substring(7) else null
       }
   }
   ```

### Phase 3: Rate Limiting

```kotlin
@Component
class RateLimitFilter(
    @Value("\${fairair.rate-limit.public.limit}") private val limit: Long
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

    private fun createBucket(): Bucket {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(limit, Duration.ofMinutes(1)))
            .build()
    }
}
```

### Phase 4: Security Headers

```kotlin
@Component
class SecurityHeadersFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        exchange.response.headers.apply {
            set("X-Content-Type-Options", "nosniff")
            set("X-Frame-Options", "DENY")
            set("X-XSS-Protection", "1; mode=block")
            set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
            set("Content-Security-Policy", buildCsp())
            set("Referrer-Policy", "strict-origin-when-cross-origin")
            set("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        }
        return chain.filter(exchange)
    }

    private fun buildCsp(): String = listOf(
        "default-src 'self'",
        "script-src 'self'",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data: https:",
        "font-src 'self'",
        "connect-src 'self' https://api.fairair.com"
    ).joinToString("; ")
}
```

---

## Testing

### Unit Tests

```kotlin
// JwtTokenProviderTest.kt
@Test
fun `should create valid access token`() {
    val token = jwtTokenProvider.createAccessToken("user-123")
    assertThat(jwtTokenProvider.validateToken(token)).isTrue()
}

@Test
fun `should reject expired token`() {
    val expiredToken = createExpiredToken()
    assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse()
}

@Test
fun `should reject tampered token`() {
    val token = jwtTokenProvider.createAccessToken("user-123")
    val tampered = token.dropLast(1) + "X"
    assertThat(jwtTokenProvider.validateToken(tampered)).isFalse()
}
```

### Integration Tests

```kotlin
// SecurityIntegrationTest.kt
@WebFluxTest
class SecurityIntegrationTest {

    @Test
    fun `unauthenticated request returns 401`() {
        webTestClient.get()
            .uri("/api/v1/bookings")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `authenticated request succeeds`() {
        val token = getValidToken()
        webTestClient.get()
            .uri("/api/v1/bookings")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `rate limited after exceeding limit`() {
        repeat(101) {
            webTestClient.get().uri("/api/v1/search").exchange()
        }
        webTestClient.get()
            .uri("/api/v1/search")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader().exists("Retry-After")
    }

    @Test
    fun `security headers present`() {
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().valueEquals("X-Frame-Options", "DENY")
            .expectHeader().exists("Content-Security-Policy")
    }
}
```

### Manual Testing

1. **Test CORS**
   ```bash
   curl -X OPTIONS http://localhost:8080/api/v1/search \
     -H "Origin: https://evil.com" \
     -H "Access-Control-Request-Method: GET" \
     -v
   # Should NOT return Access-Control-Allow-Origin
   ```

2. **Test Rate Limiting**
   ```bash
   for i in {1..110}; do
     curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/search
   done
   # Should see 429 after ~100 requests
   ```

3. **Test Security Headers**
   ```bash
   curl -I http://localhost:8080/api/v1/health
   # Verify all security headers present
   ```

4. **Test Certificate Pinning (Mobile)**
   - Configure proxy (Charles/mitmproxy) with custom certificate
   - App should fail to connect with certificate error

---

## Verification Checklist

- [ ] `runBlocking` removed from all service methods
- [ ] No hardcoded test data in production code
- [ ] CORS restricted to known origins
- [ ] Android minification enabled in release builds
- [ ] DEBUG flag is false in production
- [ ] JWT authentication on all protected endpoints
- [ ] Rate limiting returns 429 with Retry-After
- [ ] All security headers present in responses
- [ ] Certificate pinning active on mobile (test with proxy)
- [ ] No card data in logs (search logs for card patterns)
- [ ] Load test passes: 1000 concurrent users, <2s response

---

## Troubleshooting

### JWT Issues

**Problem**: Token always invalid
- Check clock skew between servers
- Verify secret key is the same everywhere
- Check token expiry settings

**Problem**: 401 on all requests
- Verify Authorization header format: `Bearer <token>`
- Check if public endpoints are excluded from auth

### Rate Limiting Issues

**Problem**: Rate limits not resetting
- Check Caffeine cache expiry settings
- Verify time window configuration

**Problem**: Legitimate users being limited
- Consider increasing limits
- Add rate limit bypass for authenticated users

### CORS Issues

**Problem**: Preflight requests failing
- Verify OPTIONS method is allowed
- Check allowed headers include required ones
- Verify origin is in allowed list

### Certificate Pinning Issues

**Problem**: App won't connect in production
- Verify certificate hash is correct
- Check backup certificate is also pinned
- Ensure development builds bypass pinning
