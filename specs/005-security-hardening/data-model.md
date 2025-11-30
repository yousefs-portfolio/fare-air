# Security Hardening Data Model

**Feature**: 005-security-hardening
**Date**: 2025-11-30
**Purpose**: Define data entities for security implementation

## Overview

This document defines the data models required for implementing security features across the FairAir platform. Models are organized by domain and include validation rules, relationships, and state transitions.

---

## Authentication Domain

### JwtClaims

**Purpose**: Represents the payload of a JWT access token.

**Location**: `shared-contract/src/commonMain/kotlin/com/flyadeal/contract/auth/JwtClaims.kt`

```kotlin
@Serializable
data class JwtClaims(
    val sub: String,           // Subject (user ID)
    val iat: Long,             // Issued at (epoch seconds)
    val exp: Long,             // Expiration (epoch seconds)
    val type: TokenType,       // ACCESS or REFRESH
    val roles: List<String> = emptyList()  // User roles
)

@Serializable
enum class TokenType {
    ACCESS,
    REFRESH
}
```

**Validation Rules**:
- `sub`: Non-empty, valid UUID format
- `iat`: Must be in the past (within clock skew tolerance of 60s)
- `exp`: Must be in the future
- `type`: Must be valid enum value
- Access token expiry: 15 minutes from `iat`
- Refresh token expiry: 7 days from `iat`

---

### LoginRequest

**Purpose**: Request payload for user authentication.

**Location**: `shared-contract/src/commonMain/kotlin/com/flyadeal/contract/auth/LoginRequest.kt`

```kotlin
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null  // For device-specific tokens
)
```

**Validation Rules**:
- `email`: Valid email format, max 255 chars
- `password`: Min 8 chars, max 128 chars
- `deviceId`: Optional, max 64 chars, alphanumeric only

---

### LoginResponse

**Purpose**: Response payload containing authentication tokens.

**Location**: `shared-contract/src/commonMain/kotlin/com/flyadeal/contract/auth/LoginResponse.kt`

```kotlin
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,          // Access token TTL in seconds
    val refreshExpiresIn: Long,   // Refresh token TTL in seconds
    val tokenType: String = "Bearer"
)
```

**Validation Rules**:
- `accessToken`: Non-empty JWT string
- `refreshToken`: Non-empty JWT string
- `expiresIn`: Positive integer (typically 900 for 15 min)
- `refreshExpiresIn`: Positive integer (typically 604800 for 7 days)

---

### TokenRefreshRequest

**Purpose**: Request to obtain new access token using refresh token.

**Location**: `shared-contract/src/commonMain/kotlin/com/flyadeal/contract/auth/TokenRefreshRequest.kt`

```kotlin
@Serializable
data class TokenRefreshRequest(
    val refreshToken: String
)
```

**Validation Rules**:
- `refreshToken`: Non-empty, valid JWT format

---

### RefreshTokenEntity (Backend Only)

**Purpose**: Server-side storage of refresh token metadata for revocation support.

**Location**: `backend-spring/src/main/kotlin/com/flyadeal/security/RefreshTokenEntity.kt`

```kotlin
data class RefreshTokenEntity(
    val tokenId: String,          // Unique token identifier (jti claim)
    val userId: String,           // Associated user ID
    val deviceId: String?,        // Optional device binding
    val createdAt: Instant,       // Token creation time
    val expiresAt: Instant,       // Token expiration time
    val revokedAt: Instant? = null,  // Null if not revoked
    val lastUsedAt: Instant? = null  // For usage tracking
)
```

**State Transitions**:
```
[Created] -> [Used] -> [Revoked]
    |                      ^
    +----------------------+
         (can revoke anytime)
```

**Validation Rules**:
- `tokenId`: UUID format
- `userId`: Non-empty, valid user reference
- `expiresAt`: Must be after `createdAt`
- Cannot be used after `revokedAt` is set

---

## Rate Limiting Domain

### RateLimitRecord

**Purpose**: Tracks request counts for rate limiting decisions.

**Location**: `backend-spring/src/main/kotlin/com/flyadeal/security/RateLimitRecord.kt`

```kotlin
data class RateLimitRecord(
    val key: String,              // IP address or user ID
    val bucket: Bucket,           // Bucket4j bucket instance
    val tier: RateLimitTier,      // Determines limits
    val windowStart: Instant,     // Current window start
    val requestCount: Long,       // Requests in current window
    val lastRequest: Instant      // Last request timestamp
)

enum class RateLimitTier {
    PUBLIC,      // 100 req/min
    AUTH,        // 10 req/min
    BOOKING,     // 10 req/min
    PAYMENT      // 5 req/min
}
```

**Storage**: Caffeine cache with 1-hour TTL after last access

---

### RateLimitResponse

**Purpose**: Response when rate limit is exceeded.

**Location**: `shared-contract/src/commonMain/kotlin/com/flyadeal/contract/security/RateLimitResponse.kt`

```kotlin
@Serializable
data class RateLimitResponse(
    val error: String = "rate_limit_exceeded",
    val message: String = "Too many requests. Please try again later.",
    val retryAfter: Long,         // Seconds until limit resets
    val limit: Long,              // Requests allowed per window
    val remaining: Long,          // Requests remaining in window
    val resetAt: Long             // Epoch timestamp when window resets
)
```

---

## Audit Logging Domain

### AuditLogEntry

**Purpose**: Records security-relevant events for audit trail.

**Location**: `backend-spring/src/main/kotlin/com/flyadeal/security/AuditLogEntry.kt`

```kotlin
data class AuditLogEntry(
    val id: String,               // Unique log entry ID
    val timestamp: Instant,       // Event timestamp
    val eventType: AuditEventType,
    val userId: String?,          // User performing action (null for anonymous)
    val ipAddress: String,        // Client IP address
    val userAgent: String?,       // Client user agent
    val resource: String,         // Affected resource (e.g., "booking/ABC123")
    val action: String,           // Action performed (e.g., "CREATE", "VIEW")
    val outcome: AuditOutcome,    // SUCCESS, FAILURE, DENIED
    val details: Map<String, String> = emptyMap(),  // Additional context
    val correlationId: String?    // Request correlation ID
)

enum class AuditEventType {
    AUTHENTICATION,
    AUTHORIZATION,
    DATA_ACCESS,
    DATA_MODIFICATION,
    RATE_LIMIT,
    SECURITY_ALERT
}

enum class AuditOutcome {
    SUCCESS,
    FAILURE,
    DENIED
}
```

**Events to Log**:
- Login attempts (success/failure)
- Token refresh
- Token revocation
- Booking creation/modification
- Payment attempts
- Rate limit hits
- Authorization failures

---

## Secure Storage Domain (Frontend)

### EncryptedBooking

**Purpose**: Structure for locally cached booking data.

**Location**: `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/security/EncryptedBooking.kt`

```kotlin
@Serializable
data class EncryptedBooking(
    val pnr: String,              // Booking reference
    val encryptedData: String,    // Base64 encoded encrypted JSON
    val storedAt: Long,           // Epoch millis when cached
    val expiresAt: Long           // Epoch millis when cache expires
)
```

**Note**: The actual booking data is encrypted at rest using platform secure storage. This wrapper tracks metadata.

---

### SecureStorageKey

**Purpose**: Defines keys used in secure storage.

**Location**: `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/security/SecureStorageKey.kt`

```kotlin
object SecureStorageKey {
    const val REFRESH_TOKEN = "fairair_refresh_token"
    const val USER_ID = "fairair_user_id"
    const val SAVED_BOOKINGS = "fairair_saved_bookings"
    const val USER_PREFERENCES = "fairair_user_prefs"
}
```

---

## Security Configuration Domain

### SecurityHeaders

**Purpose**: Defines required HTTP security headers.

**Location**: `backend-spring/src/main/kotlin/com/flyadeal/config/SecurityHeaders.kt`

```kotlin
object SecurityHeaders {
    val REQUIRED_HEADERS = mapOf(
        "X-Content-Type-Options" to "nosniff",
        "X-Frame-Options" to "DENY",
        "X-XSS-Protection" to "1; mode=block",
        "Strict-Transport-Security" to "max-age=31536000; includeSubDomains",
        "Content-Security-Policy" to "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self' https://api.fairair.com",
        "Referrer-Policy" to "strict-origin-when-cross-origin",
        "Permissions-Policy" to "geolocation=(), microphone=(), camera=()"
    )
}
```

---

### CorsConfiguration

**Purpose**: Defines allowed CORS origins.

**Location**: `backend-spring/src/main/kotlin/com/flyadeal/config/CorsConfig.kt`

```kotlin
data class CorsConfiguration(
    val allowedOrigins: List<String>,
    val allowedMethods: List<String>,
    val allowedHeaders: List<String>,
    val allowCredentials: Boolean,
    val maxAge: Long
)

// Production configuration
val productionCorsConfig = CorsConfiguration(
    allowedOrigins = listOf(
        "https://fairair.com",
        "https://www.fairair.com",
        "https://app.fairair.com"
    ),
    allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    allowedHeaders = listOf("Authorization", "Content-Type", "X-CSRF-Token"),
    allowCredentials = true,
    maxAge = 3600
)
```

---

## Entity Relationships

```
┌─────────────────────────────────────────────────────────────────┐
│                      Authentication Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  LoginRequest ──► [Auth Service] ──► LoginResponse               │
│                        │                   │                     │
│                        ▼                   ▼                     │
│              RefreshTokenEntity    JwtClaims (in token)          │
│                        │                                         │
│                        ▼                                         │
│  TokenRefreshRequest ──► [Auth Service] ──► LoginResponse        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Request Processing                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Request ──► [RateLimitFilter] ──► RateLimitRecord               │
│      │              │                    │                       │
│      │              ▼                    │ (if exceeded)         │
│      │        [JwtAuthFilter]            ▼                       │
│      │              │            RateLimitResponse               │
│      │              ▼                                            │
│      │      JwtClaims (validated)                                │
│      │              │                                            │
│      └──────────────┼──────────────────────┐                     │
│                     ▼                      ▼                     │
│              [Controller]            AuditLogEntry               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Frontend Storage                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [SecureStorage] ◄──── SecureStorageKey                          │
│        │                                                         │
│        ├──► REFRESH_TOKEN ──► String (encrypted)                 │
│        ├──► USER_ID ──► String (encrypted)                       │
│        └──► SAVED_BOOKINGS ──► EncryptedBooking[] (encrypted)    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Validation Summary

| Entity | Field | Validation |
|--------|-------|------------|
| LoginRequest | email | Required, valid email, max 255 chars |
| LoginRequest | password | Required, min 8, max 128 chars |
| JwtClaims | sub | Required, UUID format |
| JwtClaims | exp | Required, must be future |
| TokenRefreshRequest | refreshToken | Required, valid JWT |
| RateLimitResponse | retryAfter | Required, positive integer |
| AuditLogEntry | timestamp | Required, valid instant |
| AuditLogEntry | eventType | Required, valid enum |

---

## Migration Notes

No database migrations required - all data is either:
- In-memory (rate limits, refresh tokens via Caffeine)
- JWT tokens (stateless, no storage)
- Client-side encrypted storage (local to device)

Future consideration: If scaling beyond single instance, migrate Caffeine-backed stores to Redis.
