package com.flyadeal.controller

import com.flyadeal.security.JwtTokenProvider
import com.flyadeal.security.TokenValidationResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST controller for authentication endpoints.
 * Handles login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * POST /api/v1/auth/login
     * 
     * Authenticates a user and returns access/refresh tokens.
     * 
     * Note: This is a simplified implementation for demo purposes.
     * In production, validate credentials against a user database.
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        log.info("Login attempt for email: ${request.email}")
        
        // For demo: accept any valid email format
        // In production: validate against user database
        if (!isValidEmail(request.email)) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponse("INVALID_EMAIL", "Invalid email format"))
        }
        
        if (request.password.length < 6) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponse("INVALID_PASSWORD", "Password must be at least 6 characters"))
        }
        
        // Generate a user ID (in production, get from database)
        val userId = UUID.randomUUID().toString()
        
        val accessToken = jwtTokenProvider.generateAccessToken(userId, request.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userId)
        
        log.info("Login successful for user: $userId")
        
        return ResponseEntity.ok(LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = 900 // 15 minutes in seconds
        ))
    }

    /**
     * POST /api/v1/auth/refresh
     * 
     * Refreshes an access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<Any> {
        log.debug("Token refresh requested")
        
        return when (val result = jwtTokenProvider.validateToken(request.refreshToken)) {
            is TokenValidationResult.Valid -> {
                if (result.tokenType != "refresh") {
                    return ResponseEntity.badRequest()
                        .body(AuthErrorResponse("INVALID_TOKEN_TYPE", "Not a refresh token"))
                }
                
                val newAccessToken = jwtTokenProvider.generateAccessToken(
                    result.userId, 
                    result.email ?: ""
                )
                val newRefreshToken = jwtTokenProvider.generateRefreshToken(result.userId)
                
                log.debug("Token refreshed for user: ${result.userId}")
                
                ResponseEntity.ok(LoginResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    tokenType = "Bearer",
                    expiresIn = 900
                ))
            }
            is TokenValidationResult.Expired -> {
                log.debug("Refresh token expired")
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponse("TOKEN_EXPIRED", "Refresh token has expired"))
            }
            is TokenValidationResult.Invalid -> {
                log.warn("Invalid refresh token")
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponse("INVALID_TOKEN", "Invalid refresh token"))
            }
        }
    }

    /**
     * POST /api/v1/auth/logout
     * 
     * Logs out the current user.
     * In a production system with token revocation, this would invalidate the tokens.
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Any> {
        log.debug("Logout requested")
        // In production: add token to blacklist or revoke in database
        return ResponseEntity.ok(LogoutResponse(success = true, message = "Logged out successfully"))
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}

/**
 * Login request DTO.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Login/refresh response DTO.
 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

/**
 * Refresh token request DTO.
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * Logout response DTO.
 */
data class LogoutResponse(
    val success: Boolean,
    val message: String
)

/**
 * Auth error response DTO.
 */
data class AuthErrorResponse(
    val code: String,
    val message: String
)
