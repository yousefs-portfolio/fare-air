package com.flyadeal.controller

import com.flyadeal.cache.CacheService
import com.flyadeal.config.FlyadealProperties
import com.flyadeal.contract.api.ApiRoutes
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
    private val flyadealProperties: FlyadealProperties,
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
            provider = flyadealProperties.provider,
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
