package com.fairair.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT Token Provider for authentication.
 * Handles token creation, validation, and parsing.
 */
@Component
class JwtTokenProvider(
    private val securityProperties: SecurityProperties
) {
    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(securityProperties.jwt.secret.toByteArray())
    }

    /**
     * Generates an access token for the given user.
     * @param userId The user's unique identifier
     * @param email The user's email address
     * @return JWT access token string
     */
    fun generateAccessToken(userId: String, email: String): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(securityProperties.jwt.accessTokenExpirySeconds)
        
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    /**
     * Generates a refresh token for the given user.
     * @param userId The user's unique identifier
     * @return JWT refresh token string
     */
    fun generateRefreshToken(userId: String): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(securityProperties.jwt.refreshTokenExpirySeconds)
        
        return Jwts.builder()
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    /**
     * Validates and parses a JWT token.
     * @param token The JWT token string
     * @return TokenValidationResult with success/failure and claims
     */
    fun validateToken(token: String): TokenValidationResult {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            
            TokenValidationResult.Valid(
                userId = claims.subject,
                email = claims["email"] as? String,
                tokenType = claims["type"] as? String ?: "access"
            )
        } catch (e: ExpiredJwtException) {
            log.debug("Token expired: ${e.message}")
            TokenValidationResult.Expired
        } catch (e: JwtException) {
            log.warn("Invalid token: ${e.message}")
            TokenValidationResult.Invalid(e.message ?: "Invalid token")
        } catch (e: Exception) {
            log.error("Token validation error: ${e.message}")
            TokenValidationResult.Invalid("Token validation failed")
        }
    }

    /**
     * Extracts the token type (access/refresh) from a token.
     * @param token The JWT token string
     * @return The token type or null if invalid
     */
    fun getTokenType(token: String): String? {
        return when (val result = validateToken(token)) {
            is TokenValidationResult.Valid -> result.tokenType
            else -> null
        }
    }
}

/**
 * Result of token validation.
 */
sealed class TokenValidationResult {
    /**
     * Token is valid.
     */
    data class Valid(
        val userId: String,
        val email: String?,
        val tokenType: String
    ) : TokenValidationResult()

    /**
     * Token is expired.
     */
    data object Expired : TokenValidationResult()

    /**
     * Token is invalid (malformed, bad signature, etc.)
     */
    data class Invalid(val reason: String) : TokenValidationResult()
}
