package com.flyadeal.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Cache configuration using Caffeine.
 *
 * Provides caching for:
 * - Routes and stations: 24-hour TTL
 * - Search results: 5-minute TTL
 *
 * Uses Caffeine directly without Spring's @Cacheable abstraction
 * to match the Quarkus implementation's manual cache management.
 */
@Configuration
class CacheConfig(
    private val flyadealProperties: FlyadealProperties
) {
    companion object {
        const val ROUTES_CACHE = "routes"
        const val STATIONS_CACHE = "stations"
        const val SEARCHES_CACHE = "searches"
    }

    @Bean
    fun routesCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(flyadealProperties.cache.routesTtl))
    }

    @Bean
    fun stationsCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(flyadealProperties.cache.routesTtl))
    }

    @Bean
    fun searchesCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(flyadealProperties.cache.searchTtl))
    }

    private fun buildCache(ttl: Duration): Cache<String, Any> {
        return Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(1000)
            .recordStats()
            .build()
    }
}
