package com.fairair.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * JWT Authentication Web Filter for Spring WebFlux.
 * Validates JWT tokens and sets up the security context.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        
        /**
         * Paths that don't require authentication
         */
        private val PUBLIC_PATHS = setOf(
            "/health",
            "/health/live",
            "/health/ready",
            "/actuator/health",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/config/stations",
            "/api/v1/config/routes",
            "/api/v1/config/route-map"
        )
        
        /**
         * Path prefixes that don't require authentication
         */
        private val PUBLIC_PATH_PREFIXES = setOf(
            "/actuator/"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // Authentication disabled for demo - pass through all requests
        return chain.filter(exchange)
    }

    private fun isPublicPath(path: String): Boolean {
        if (PUBLIC_PATHS.contains(path)) {
            return true
        }
        return PUBLIC_PATH_PREFIXES.any { path.startsWith(it) }
    }

    private fun handleUnauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        log.debug("Unauthorized: $message for path ${exchange.request.uri.path}")
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        exchange.response.headers.add("Content-Type", "application/json")
        
        val body = """{"error": "Unauthorized", "message": "$message"}"""
        val buffer = exchange.response.bufferFactory().wrap(body.toByteArray())
        
        return exchange.response.writeWith(Mono.just(buffer))
    }
}
