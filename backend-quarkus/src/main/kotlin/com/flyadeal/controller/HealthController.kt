package com.flyadeal.controller

import com.flyadeal.cache.CacheService
import com.flyadeal.config.FlyadealConfig
import com.flyadeal.contract.api.ApiRoutes
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.logging.Logger

/**
 * Health check endpoint for monitoring and load balancer probes.
 */
@Path(ApiRoutes.Health.CHECK)
@Produces(MediaType.APPLICATION_JSON)
class HealthController {

    @Inject
    lateinit var config: FlyadealConfig

    @Inject
    lateinit var cacheService: CacheService

    private val log = Logger.getLogger(HealthController::class.java)

    /**
     * GET /health
     *
     * Returns application health status.
     *
     * @return HealthResponse with status and details
     */
    @GET
    fun health(): HealthResponse {
        log.debug("Health check requested")

        val cacheStats = cacheService.getStats()

        return HealthResponse(
            status = "UP",
            provider = config.provider(),
            cache = CacheHealthInfo(
                routeMapHitRate = formatPercent(cacheStats.routeMapHitRate),
                stationsHitRate = formatPercent(cacheStats.stationsHitRate),
                searchHitRate = formatPercent(cacheStats.searchHitRate),
                searchCacheSize = cacheStats.searchSize
            )
        )
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
