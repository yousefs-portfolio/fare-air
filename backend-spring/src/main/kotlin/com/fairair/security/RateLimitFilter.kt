package com.fairair.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting web filter using Bucket4j.
 * Implements token bucket algorithm for per-IP rate limiting.
 */
@Component
class RateLimitFilter(
    private val securityProperties: SecurityProperties
) : WebFilter {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    // In-memory bucket storage (use distributed cache like Redis in production)
    private val generalBuckets = ConcurrentHashMap<String, Bucket>()
    private val sensitiveBuckets = ConcurrentHashMap<String, Bucket>()

    companion object {
        /**
         * Paths with stricter rate limits
         */
        private val SENSITIVE_PATHS = setOf(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/booking"
        )
        
        /**
         * Path prefixes that are exempt from rate limiting
         */
        private val EXEMPT_PATHS = setOf(
            "/health",
            "/actuator"
        )
        
        // Maximum number of tracked IPs to prevent memory exhaustion
        private const val MAX_TRACKED_IPS = 100000
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!securityProperties.rateLimit.enabled) {
            return chain.filter(exchange)
        }
        
        val path = exchange.request.uri.path
        
        // Skip rate limiting for exempt paths
        if (EXEMPT_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val clientIp = getClientIp(exchange)
        val isSensitive = isSensitivePath(path)
        
        val bucket = getBucket(clientIp, isSensitive)
        
        return if (bucket.tryConsume(1)) {
            // Request allowed
            chain.filter(exchange)
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: $clientIp on path: $path")
            handleRateLimitExceeded(exchange)
        }
    }

    private fun getClientIp(exchange: ServerWebExchange): String {
        // Check for forwarded header (behind load balancer/proxy)
        val forwardedFor = exchange.request.headers.getFirst("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",").first().trim()
        }
        
        val realIp = exchange.request.headers.getFirst("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }
        
        return exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    private fun isSensitivePath(path: String): Boolean {
        return SENSITIVE_PATHS.any { path.startsWith(it) }
    }

    private fun getBucket(clientIp: String, sensitive: Boolean): Bucket {
        val bucketMap = if (sensitive) sensitiveBuckets else generalBuckets
        val requestsPerMinute = if (sensitive) {
            securityProperties.rateLimit.sensitiveRequestsPerMinute
        } else {
            securityProperties.rateLimit.generalRequestsPerMinute
        }
        
        // Prevent memory exhaustion by limiting tracked IPs
        if (bucketMap.size >= MAX_TRACKED_IPS && !bucketMap.containsKey(clientIp)) {
            // Clean up old entries (simple eviction - in production use proper cache)
            val toRemove = bucketMap.keys.take(MAX_TRACKED_IPS / 10)
            toRemove.forEach { bucketMap.remove(it) }
        }
        
        return bucketMap.computeIfAbsent(clientIp) {
            createBucket(requestsPerMinute)
        }
    }

    private fun createBucket(requestsPerMinute: Long): Bucket {
        val bandwidth = Bandwidth.builder()
            .capacity(requestsPerMinute)
            .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
            .build()
        
        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }

    private fun handleRateLimitExceeded(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        exchange.response.headers.add("Content-Type", "application/json")
        exchange.response.headers.add("Retry-After", "60") // Retry after 60 seconds
        
        val body = """{"error": "TOO_MANY_REQUESTS", "message": "Rate limit exceeded. Please try again later.", "retryAfterSeconds": 60}"""
        val buffer = exchange.response.bufferFactory().wrap(body.toByteArray())
        
        return exchange.response.writeWith(Mono.just(buffer))
    }
}
