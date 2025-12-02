package com.fairair.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Security configuration properties.
 * Maps to the fairair.security.* prefix in application.yml.
 */
@ConfigurationProperties(prefix = "fairair.security")
data class SecurityProperties(
    /**
     * JWT configuration
     */
    val jwt: JwtProperties = JwtProperties(),
    
    /**
     * Rate limiting configuration
     */
    val rateLimit: RateLimitProperties = RateLimitProperties(),
    
    /**
     * CORS configuration
     */
    val cors: CorsProperties = CorsProperties()
)

/**
 * JWT-specific properties
 */
data class JwtProperties(
    /**
     * Secret key for signing JWT tokens.
     * MUST be at least 256 bits (32 characters) for HS256.
     * In production, use a strong random value from environment variable.
     */
    val secret: String = "change-this-in-production-must-be-at-least-32-chars",
    
    /**
     * Access token expiry in seconds (default: 15 minutes)
     */
    val accessTokenExpirySeconds: Long = 900,
    
    /**
     * Refresh token expiry in seconds (default: 7 days)
     */
    val refreshTokenExpirySeconds: Long = 604800
)

/**
 * Rate limiting properties
 */
data class RateLimitProperties(
    /**
     * Whether rate limiting is enabled
     */
    val enabled: Boolean = true,
    
    /**
     * Requests per minute for general endpoints
     */
    val generalRequestsPerMinute: Long = 100,
    
    /**
     * Requests per minute for sensitive endpoints (auth, payment)
     */
    val sensitiveRequestsPerMinute: Long = 10
)

/**
 * CORS properties
 */
data class CorsProperties(
    /**
     * Allowed origins for CORS.
     * In production, should be specific domains only.
     */
    val allowedOrigins: List<String> = listOf(
        "http://localhost:8081",
        "http://localhost:3000",
        "http://127.0.0.1:8081"
    ),
    
    /**
     * Allowed HTTP methods
     */
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    
    /**
     * Max age for preflight cache in seconds
     */
    val maxAge: Long = 86400
)
