package com.flyadeal.cache

import com.flyadeal.contract.model.FlightResponse
import com.flyadeal.contract.model.RouteMap
import com.flyadeal.contract.model.Station
import com.github.benmanes.caffeine.cache.Cache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for caching frequently accessed data using Caffeine.
 * Implements caching for routes, stations, and flight search results.
 * 
 * Provides suspend-aware cache retrieval methods to avoid blocking
 * Netty event loop threads in WebFlux applications.
 */
@Service
class CacheService(
    private val routesCache: Cache<String, Any>,
    private val stationsCache: Cache<String, Any>,
    private val searchesCache: Cache<String, Any>
) {
    private val log = LoggerFactory.getLogger(CacheService::class.java)
    
    // Mutexes to prevent thundering herd on cache misses
    private val routesMutex = Mutex()
    private val stationsMutex = Mutex()

    companion object {
        private const val ROUTES_KEY = "routes"
        private const val STATIONS_KEY = "stations"
    }

    /**
     * Gets the cached route map, or fetches it using the suspend provider if not cached.
     * Uses coroutine-safe locking to prevent thundering herd on cache miss.
     * @param fetcher Suspend function to fetch the route map if not in cache
     * @return The RouteMap
     */
    suspend fun getRouteMapSuspend(fetcher: suspend () -> RouteMap): RouteMap {
        // Fast path: check cache without lock
        routesCache.getIfPresent(ROUTES_KEY)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as RouteMap
        }
        
        // Slow path: acquire lock and double-check
        return routesMutex.withLock {
            routesCache.getIfPresent(ROUTES_KEY)?.let {
                @Suppress("UNCHECKED_CAST")
                return@withLock it as RouteMap
            }
            
            log.debug("Route map cache miss, fetching from provider")
            val result = fetcher()
            routesCache.put(ROUTES_KEY, result)
            result
        }
    }

    /**
     * Gets the cached stations, or fetches them using the suspend provider if not cached.
     * Uses coroutine-safe locking to prevent thundering herd on cache miss.
     * @param fetcher Suspend function to fetch the stations if not in cache
     * @return List of Stations
     */
    suspend fun getStationsSuspend(fetcher: suspend () -> List<Station>): List<Station> {
        // Fast path: check cache without lock
        stationsCache.getIfPresent(STATIONS_KEY)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as List<Station>
        }
        
        // Slow path: acquire lock and double-check
        return stationsMutex.withLock {
            stationsCache.getIfPresent(STATIONS_KEY)?.let {
                @Suppress("UNCHECKED_CAST")
                return@withLock it as List<Station>
            }
            
            log.debug("Stations cache miss, fetching from provider")
            val result = fetcher()
            stationsCache.put(STATIONS_KEY, result)
            result
        }
    }

    /**
     * Gets the cached route map, or fetches it using the provider if not cached.
     * @deprecated Use getRouteMapSuspend for non-blocking cache retrieval
     * @param fetcher Function to fetch the route map if not in cache
     * @return The RouteMap
     */
    @Deprecated("Use getRouteMapSuspend for non-blocking cache retrieval", ReplaceWith("getRouteMapSuspend(fetcher)"))
    fun getRouteMap(fetcher: () -> RouteMap): RouteMap {
        @Suppress("UNCHECKED_CAST")
        return routesCache.get(ROUTES_KEY) { _ ->
            log.debug("Route map cache miss, fetching from provider")
            fetcher()
        } as RouteMap
    }

    /**
     * Gets the cached stations, or fetches them using the provider if not cached.
     * @deprecated Use getStationsSuspend for non-blocking cache retrieval
     * @param fetcher Function to fetch the stations if not in cache
     * @return List of Stations
     */
    @Deprecated("Use getStationsSuspend for non-blocking cache retrieval", ReplaceWith("getStationsSuspend(fetcher)"))
    fun getStations(fetcher: () -> List<Station>): List<Station> {
        @Suppress("UNCHECKED_CAST")
        return stationsCache.get(STATIONS_KEY) { _ ->
            log.debug("Stations cache miss, fetching from provider")
            fetcher()
        } as List<Station>
    }

    /**
     * Caches a flight search response with its search ID.
     * @param searchId The unique search identifier
     * @param response The flight response to cache
     */
    fun cacheSearchResult(searchId: String, response: FlightResponse) {
        log.debug("Caching search result for searchId=$searchId")
        searchesCache.put(searchId, response)
    }

    /**
     * Retrieves a cached search result by its ID.
     * @param searchId The search identifier
     * @return The cached FlightResponse, or null if not found/expired
     */
    fun getSearchResult(searchId: String): FlightResponse? {
        return searchesCache.getIfPresent(searchId) as? FlightResponse
    }

    /**
     * Invalidates all cached data. Primarily used for testing.
     */
    fun invalidateAll() {
        log.info("Invalidating all caches")
        routesCache.invalidateAll()
        stationsCache.invalidateAll()
        searchesCache.invalidateAll()
    }

    /**
     * Returns cache statistics for monitoring.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            routeMapHitRate = routesCache.stats().hitRate(),
            stationsHitRate = stationsCache.stats().hitRate(),
            searchHitRate = searchesCache.stats().hitRate(),
            searchSize = searchesCache.estimatedSize()
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
