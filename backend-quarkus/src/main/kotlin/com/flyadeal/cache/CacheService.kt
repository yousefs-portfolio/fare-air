package com.flyadeal.cache

import com.flyadeal.config.FlyadealConfig
import com.flyadeal.contract.model.FlightResponse
import com.flyadeal.contract.model.RouteMap
import com.flyadeal.contract.model.Station
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * Service for caching frequently accessed data using Caffeine.
 * Implements caching for routes, stations, and flight search results.
 */
@ApplicationScoped
class CacheService {

    @Inject
    lateinit var config: FlyadealConfig

    private val log = Logger.getLogger(CacheService::class.java)

    /**
     * Cache for the route map (single entry, keyed by constant).
     * TTL: 24 hours (configurable)
     */
    private lateinit var routeMapCache: Cache<String, RouteMap>

    /**
     * Cache for all stations (single entry, keyed by constant).
     * TTL: 24 hours (configurable)
     */
    private lateinit var stationsCache: Cache<String, List<Station>>

    /**
     * Cache for flight search results, keyed by search ID.
     * TTL: 5 minutes (configurable)
     */
    private lateinit var searchCache: Cache<String, FlightResponse>

    companion object {
        private const val ROUTES_KEY = "routes"
        private const val STATIONS_KEY = "stations"
        private const val MAX_SEARCH_ENTRIES = 1000L
    }

    @PostConstruct
    fun init() {
        log.info("Initializing cache service with routes TTL=${config.cache().routesTtl()}s, search TTL=${config.cache().searchTtl()}s")

        routeMapCache = Caffeine.newBuilder()
            .expireAfterWrite(config.cache().routesTtl(), TimeUnit.SECONDS)
            .maximumSize(1)
            .recordStats()
            .build()

        stationsCache = Caffeine.newBuilder()
            .expireAfterWrite(config.cache().routesTtl(), TimeUnit.SECONDS)
            .maximumSize(1)
            .recordStats()
            .build()

        searchCache = Caffeine.newBuilder()
            .expireAfterWrite(config.cache().searchTtl(), TimeUnit.SECONDS)
            .maximumSize(MAX_SEARCH_ENTRIES)
            .recordStats()
            .build()
    }

    /**
     * Gets the cached route map, or fetches it using the provider if not cached.
     * @param fetcher Function to fetch the route map if not in cache
     * @return The RouteMap
     */
    fun getRouteMap(fetcher: () -> RouteMap): RouteMap {
        return routeMapCache.get(ROUTES_KEY) { _ ->
            log.debug("Route map cache miss, fetching from provider")
            fetcher()
        }!!
    }

    /**
     * Gets the cached stations, or fetches them using the provider if not cached.
     * @param fetcher Function to fetch the stations if not in cache
     * @return List of Stations
     */
    fun getStations(fetcher: () -> List<Station>): List<Station> {
        return stationsCache.get(STATIONS_KEY) { _ ->
            log.debug("Stations cache miss, fetching from provider")
            fetcher()
        }!!
    }

    /**
     * Caches a flight search response with its search ID.
     * @param searchId The unique search identifier
     * @param response The flight response to cache
     */
    fun cacheSearchResult(searchId: String, response: FlightResponse) {
        log.debug("Caching search result for searchId=$searchId")
        searchCache.put(searchId, response)
    }

    /**
     * Retrieves a cached search result by its ID.
     * @param searchId The search identifier
     * @return The cached FlightResponse, or null if not found/expired
     */
    fun getSearchResult(searchId: String): FlightResponse? {
        return searchCache.getIfPresent(searchId)
    }

    /**
     * Invalidates all cached data. Primarily used for testing.
     */
    fun invalidateAll() {
        log.info("Invalidating all caches")
        routeMapCache.invalidateAll()
        stationsCache.invalidateAll()
        searchCache.invalidateAll()
    }

    /**
     * Returns cache statistics for monitoring.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            routeMapHitRate = routeMapCache.stats().hitRate(),
            stationsHitRate = stationsCache.stats().hitRate(),
            searchHitRate = searchCache.stats().hitRate(),
            searchSize = searchCache.estimatedSize()
        )
    }
}

/**
 * Cache statistics for monitoring and debugging.
 */
data class CacheStats(
    val routeMapHitRate: Double,
    val stationsHitRate: Double,
    val searchHitRate: Double,
    val searchSize: Long
)
