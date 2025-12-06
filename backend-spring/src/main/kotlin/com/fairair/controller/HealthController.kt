package com.fairair.controller

import com.fairair.cache.CacheService
import com.fairair.config.FairairProperties
import com.fairair.contract.api.ApiRoutes
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Health check endpoint for monitoring and load balancer probes.
 *
 * Provides custom health endpoints that work alongside Spring Boot Actuator.
 * These endpoints maintain API compatibility with the original Quarkus implementation.
 */
@RestController
class HealthController(
    private val fairairProperties: FairairProperties,
    private val cacheService: CacheService
) {
    private val log = LoggerFactory.getLogger(HealthController::class.java)

    /**
     * GET /health
     *
     * Returns application health status with cache statistics.
     *
     * @return HealthResponse with status and details
     */
    @GetMapping(ApiRoutes.Health.CHECK)
    fun health(): HealthResponse {
        log.debug("Health check requested")

        val cacheStats = cacheService.getStats()

        return HealthResponse(
            status = "UP",
            provider = fairairProperties.provider,
            cache = CacheHealthInfo(
                routeMapHitRate = formatPercent(cacheStats.routeMapHitRate),
                stationsHitRate = formatPercent(cacheStats.stationsHitRate),
                searchHitRate = formatPercent(cacheStats.searchHitRate),
                searchCacheSize = cacheStats.searchSize
            )
        )
    }

    /**
     * GET /health/live
     *
     * Liveness probe for Kubernetes.
     * Returns 200 if the application is running.
     */
    @GetMapping("/health/live")
    fun liveness(): ResponseEntity<LivenessResponse> {
        return ResponseEntity.ok(LivenessResponse(status = "UP"))
    }

    /**
     * GET /health/ready
     *
     * Readiness probe for Kubernetes.
     * Returns 200 if the application is ready to receive traffic.
     */
    @GetMapping("/health/ready")
    fun readiness(): ResponseEntity<ReadinessResponse> {
        return ResponseEntity.ok(ReadinessResponse(status = "UP"))
    }

    /**
     * DELETE /health/cache
     *
     * Invalidates all caches. Useful for development and testing.
     * Forces re-fetch of routes, stations, and clears search caches.
     */
    @DeleteMapping("/health/cache")
    fun invalidateCache(): ResponseEntity<Map<String, String>> {
        log.info("Cache invalidation requested")
        cacheService.invalidateAll()
        return ResponseEntity.ok(mapOf("status" to "cache invalidated"))
    }

    private fun formatPercent(rate: Double): String {
        return if (rate.isNaN()) "N/A" else "${(rate * 100).toInt()}%"
    }
}

/**
 * Health check response.
 */
data class HealthResponse(
    val status: String,
    val provider: String,
    val cache: CacheHealthInfo
)

/**
 * Cache health information.
 */
data class CacheHealthInfo(
    val routeMapHitRate: String,
    val stationsHitRate: String,
    val searchHitRate: String,
    val searchCacheSize: Long
)

/**
 * Liveness probe response.
 */
data class LivenessResponse(
    val status: String
)

/**
 * Readiness probe response.
 */
data class ReadinessResponse(
    val status: String
)
