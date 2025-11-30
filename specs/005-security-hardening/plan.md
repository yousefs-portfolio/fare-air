# Implementation Plan: Security Hardening

**Branch**: `005-security-hardening` | **Date**: 2025-11-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-security-hardening/spec.md`

## Summary

This implementation plan addresses 34 security vulnerabilities identified across the FairAir airline booking platform. The primary technical approach involves:

1. **Backend Security** (Spring Boot WebFlux): JWT authentication, CORS hardening, rate limiting, security headers, thread safety fixes
2. **Frontend Security** (KMP/Compose): Payment tokenization, certificate pinning, encrypted storage, build hardening
3. **Configuration**: Environment-based secrets, debug flag management, minification enablement

The goal is to achieve zero CRITICAL and HIGH severity findings in a re-scan while maintaining system performance (1000 concurrent users, <2s response time).

## Technical Context

**Language/Version**: Kotlin 2.0.21 (K2 compiler)
**Primary Dependencies**:
- Backend: Spring Boot 3.2 WebFlux, Spring Security, Caffeine, kotlinx-coroutines
- Frontend: Compose Multiplatform 1.7.1, Voyager 1.1.0, Koin 4.0.0, Ktor Client 3.0.1
**Storage**: Caffeine (in-memory cache), LocalStorage (frontend), EncryptedSharedPreferences (Android), Keychain (iOS)
**Testing**: JUnit 5, Kotlin Test, Spring WebFlux Test
**Target Platform**:
- Backend: JVM (Linux server)
- Frontend: Android, iOS, Web (WASM)
**Project Type**: Multi-module (backend + frontend + shared-contract)
**Performance Goals**: 1000 concurrent users, <2s p95 response time, zero thread exhaustion
**Constraints**: PCI-DSS compliance for payment handling, OWASP Top 10 compliance
**Scale/Scope**: 3 modules (shared-contract, backend-spring, apps-kmp), ~50 screens, 10k+ daily users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on CLAUDE.md project guidelines:

| Principle | Status | Notes |
|-----------|--------|-------|
| No Stubs/Placeholders | PASS | All security implementations will be production-ready |
| Production Standards | PASS | Complete error handling, logging, edge cases |
| Shared Contract Source of Truth | PASS | Security DTOs defined in :shared-contract |
| Business Logic in ScreenModel | PASS | Security logic in services/repositories, not UI |
| Backend Layering | PASS | Security filters → Controllers → Services → Clients |

**Gate Result**: PASS - No violations. Proceeding to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/005-security-hardening/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file
├── checklist.md         # Security verification checklist (complete)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
# Existing project structure with security additions

shared-contract/
└── src/commonMain/kotlin/com/flyadeal/contract/
    ├── auth/                    # NEW: Authentication DTOs
    │   ├── LoginRequest.kt
    │   ├── LoginResponse.kt
    │   ├── TokenRefreshRequest.kt
    │   └── JwtClaims.kt
    └── security/                # NEW: Security-related DTOs
        └── RateLimitResponse.kt

backend-spring/
└── src/main/kotlin/com/flyadeal/
    ├── config/
    │   ├── SecurityConfig.kt        # NEW: Spring Security configuration
    │   ├── CorsConfig.kt            # MODIFY: Restrict origins
    │   ├── RateLimitConfig.kt       # NEW: Rate limiting setup
    │   └── CacheConfig.kt           # MODIFY: Bound cache size
    ├── security/                    # NEW: Security components
    │   ├── JwtTokenProvider.kt
    │   ├── JwtAuthenticationFilter.kt
    │   ├── RateLimitFilter.kt
    │   └── SecurityHeadersFilter.kt
    ├── service/
    │   └── FlightService.kt         # MODIFY: Remove runBlocking
    └── controller/
        └── AuthController.kt        # NEW: Authentication endpoints

apps-kmp/
└── src/
    ├── commonMain/kotlin/com/flyadeal/app/
    │   ├── security/                # NEW: Security utilities
    │   │   ├── TokenManager.kt
    │   │   ├── SecureStorage.kt
    │   │   └── PaymentTokenizer.kt
    │   ├── network/
    │   │   └── HttpClientConfig.kt  # MODIFY: Add auth headers, cert pinning
    │   └── ui/screens/payment/
    │       └── PaymentScreen.kt     # MODIFY: Remove hardcoded card
    ├── androidMain/kotlin/
    │   └── security/
    │       ├── AndroidSecureStorage.kt  # NEW: EncryptedSharedPrefs
    │       └── CertificatePinner.kt     # NEW: OkHttp pinning
    └── iosMain/kotlin/
        └── security/
            ├── IosSecureStorage.kt      # NEW: Keychain access
            └── CertificatePinner.kt     # NEW: NSURLSession pinning
```

**Structure Decision**: Follows existing multi-module architecture. Security components added to each module as appropriate:
- Shared DTOs in :shared-contract
- Backend security filters/services in :backend-spring
- Platform-specific secure storage in :apps-kmp platform source sets

## Complexity Tracking

> No constitution violations requiring justification.

N/A - Design follows existing patterns without introducing additional complexity.

---

## Phase 0: Research Summary

### R1: JWT Authentication for Spring WebFlux

**Decision**: Use Spring Security with custom JWT filter for WebFlux

**Rationale**:
- Spring Security is the standard for Spring Boot applications
- WebFlux requires non-blocking filter implementation
- JWT is stateless, works well with reactive stack

**Alternatives Considered**:
- Session-based auth: Rejected - requires sticky sessions, doesn't scale
- OAuth2/OIDC: Overkill for current needs, adds external dependency
- API Keys: Less secure, harder to revoke/rotate

**Implementation Notes**:
- Use `io.jsonwebtoken:jjwt` for JWT creation/validation
- Access tokens: 15 min expiry, contains userId only
- Refresh tokens: 7 day expiry, stored server-side with revocation support
- Use `WebFilter` interface for reactive pipeline integration

### R2: Rate Limiting Strategy

**Decision**: Bucket4j with Caffeine backend for in-memory rate limiting

**Rationale**:
- Bucket4j is battle-tested, supports token bucket algorithm
- Caffeine integration for distributed-ready caching
- Supports per-IP and per-user limiting

**Alternatives Considered**:
- Redis-based (Resilience4j): Good for distributed, but adds infrastructure
- Custom implementation: Error-prone, not worth the effort
- API Gateway limiting: Not using gateway currently

**Implementation Notes**:
- General endpoints: 100 req/min/IP
- Auth endpoints: 10 req/min/IP (brute force protection)
- Payment endpoints: 5 req/min/user (fraud prevention)
- Return 429 with Retry-After header

### R3: Certificate Pinning for KMP

**Decision**: Platform-specific implementation using OkHttp (Android) and NSURLSession (iOS)

**Rationale**:
- Ktor doesn't have built-in pinning for all platforms
- OkHttp is already used by Ktor on Android
- NSURLSession native pinning is most reliable on iOS

**Alternatives Considered**:
- TrustKit library: iOS only, adds dependency
- Custom X509TrustManager: Low-level, error-prone
- Public key pinning: More complex than certificate pinning

**Implementation Notes**:
- Pin to 2 certificates (current + backup)
- Use SHA-256 fingerprints
- Implement graceful fallback for development builds
- WebWasm uses browser's certificate validation

### R4: Payment Tokenization

**Decision**: Client-side tokenization using Stripe Elements (or Adyen Web Components)

**Rationale**:
- PCI-DSS compliance requires card data never touches our servers
- Stripe/Adyen provide drop-in UI components
- Tokens are processed server-side with payment provider SDK

**Alternatives Considered**:
- Server-side tokenization: Increases PCI scope significantly
- 3DS only: Doesn't address card storage issue
- Custom encryption: Not PCI compliant

**Implementation Notes**:
- For current implementation (mock), remove hardcoded card entirely
- Replace with placeholder UI that explains "Payment integration pending"
- When real payment added, use provider's native SDK
- Never store/transmit raw card numbers

### R5: Encrypted Storage for Mobile

**Decision**:
- Android: EncryptedSharedPreferences (Jetpack Security)
- iOS: Keychain Services via expect/actual

**Rationale**:
- Both are platform-recommended secure storage solutions
- Hardware-backed encryption on modern devices
- No additional dependencies needed

**Alternatives Considered**:
- SQLCipher: Overkill for key-value storage
- Custom AES encryption: Error-prone, key management issues
- Third-party libraries: Adds attack surface

**Implementation Notes**:
- Use expect/actual pattern for cross-platform interface
- Store: refresh tokens, saved booking data, user preferences
- Never store: passwords, card data, session tokens (use memory)

### R6: Thread Safety - Removing runBlocking

**Decision**: Convert FlightService to fully suspend functions, use `coroutineScope` for parallel operations

**Rationale**:
- `runBlocking` blocks Netty event loop threads
- WebFlux is designed for non-blocking operations
- Coroutines integrate naturally with reactive streams

**Alternatives Considered**:
- Move to separate thread pool: Band-aid, doesn't solve root cause
- Use Reactor directly: Mixes paradigms unnecessarily
- Async wrappers: Still blocks threads internally

**Implementation Notes**:
- Change service functions to `suspend fun`
- Use `coroutineScope { }` for parallel calls
- Use `flow { }` for streaming responses
- Controllers use `@ResponseBody suspend fun` pattern

---

## Phase 1: Design Artifacts

### Data Model

See [data-model.md](./data-model.md) for complete entity definitions including:
- JwtToken (access/refresh token structure)
- RateLimitRecord (per-IP/user rate tracking)
- AuditLogEntry (security event logging)
- EncryptedBooking (secure local storage format)

### API Contracts

See [contracts/](./contracts/) for OpenAPI specifications:
- `auth-api.yaml` - Authentication endpoints (login, refresh, logout)
- `security-headers.yaml` - Required response headers
- `rate-limit-api.yaml` - Rate limit response format

### Quickstart Guide

See [quickstart.md](./quickstart.md) for:
- Environment setup for security features
- JWT secret configuration
- Certificate pinning setup
- Testing security implementations

---

## Implementation Phases

### Phase 1: Critical Fixes (MUST complete first)
1. Remove `runBlocking` from FlightService
2. Implement JWT authentication
3. Remove hardcoded test card from PaymentScreen
4. Restrict CORS to specific origins
5. Enable Android release minification
6. Make DEBUG flag configurable

### Phase 2: High Priority Security
1. Implement CSRF protection
2. Add rate limiting
3. Add security headers
4. Scope bookings to authenticated user
5. Remove sensitive data from logs
6. Add certificate pinning
7. Bound Caffeine cache size
8. Add external API timeouts

### Phase 3: Medium Priority Hardening
1. Implement encrypted storage
2. Add secure cookie flags
3. Sanitize error messages
4. Add audit logging
5. Use cryptographic booking references

### Phase 4: Low Priority Polish
1. Add remaining security headers
2. Remove verbose stack traces
3. Add Feature-Policy headers
4. Clean up code comments

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| JWT implementation bugs | Critical - Auth bypass | Comprehensive testing, use battle-tested library |
| Certificate pinning breaks app | High - App unusable | Include backup pins, staged rollout |
| Rate limiting too aggressive | Medium - User frustration | Monitor metrics, adjustable limits |
| Performance degradation from security | Medium - UX impact | Load test at each phase |

## Dependencies

External libraries to add:
- `io.jsonwebtoken:jjwt-api:0.12.x` - JWT handling
- `io.jsonwebtoken:jjwt-impl:0.12.x` - JWT implementation
- `io.jsonwebtoken:jjwt-jackson:0.12.x` - JWT JSON support
- `com.bucket4j:bucket4j-core:8.x` - Rate limiting
- `androidx.security:security-crypto:1.1.x` - Android encrypted storage

## Success Metrics

- [ ] Zero CRITICAL findings on re-scan
- [ ] Zero HIGH findings on re-scan
- [ ] All API endpoints require authentication (except health/public)
- [ ] Load test passes: 1000 concurrent users, <2s p95
- [ ] No card data in logs (verified by log search)
- [ ] Certificate pinning active (verified by MITM test)
