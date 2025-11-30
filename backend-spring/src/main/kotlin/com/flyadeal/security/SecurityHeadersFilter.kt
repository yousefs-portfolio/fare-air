package com.flyadeal.security

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Security headers web filter.
 * Adds essential security headers to all responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SecurityHeadersFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val headers = exchange.response.headers
        
        // Prevent clickjacking
        headers.add("X-Frame-Options", "DENY")
        
        // Prevent MIME type sniffing
        headers.add("X-Content-Type-Options", "nosniff")
        
        // XSS protection (legacy, but still useful for older browsers)
        headers.add("X-XSS-Protection", "1; mode=block")
        
        // Enforce HTTPS (in production)
        // 1 year = 31536000 seconds
        headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        
        // Content Security Policy - restrictive default
        headers.add(
            "Content-Security-Policy",
            buildString {
                append("default-src 'self'; ")
                append("script-src 'self'; ")
                append("style-src 'self' 'unsafe-inline'; ")
                append("img-src 'self' data: https:; ")
                append("font-src 'self'; ")
                append("connect-src 'self'; ")
                append("frame-ancestors 'none'; ")
                append("form-action 'self'; ")
                append("base-uri 'self'")
            }
        )
        
        // Referrer policy - send referrer for same-origin, nothing for cross-origin
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin")
        
        // Permissions policy (formerly Feature-Policy)
        headers.add(
            "Permissions-Policy",
            buildString {
                append("camera=(), ")
                append("microphone=(), ")
                append("geolocation=(), ")
                append("payment=(self)")
            }
        )
        
        // Don't cache sensitive responses by default
        if (!isCacheablePath(exchange.request.uri.path)) {
            headers.add("Cache-Control", "no-store, no-cache, must-revalidate, private")
            headers.add("Pragma", "no-cache")
        }
        
        return chain.filter(exchange)
    }
    
    private fun isCacheablePath(path: String): Boolean {
        // Allow caching for static/public config endpoints
        return path.startsWith("/api/v1/config/") || 
               path.startsWith("/health")
    }
}
