package com.flyadeal.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the flyadeal application.
 * Maps to the flyadeal.* prefix in application.yml.
 */
@ConfigurationProperties(prefix = "flyadeal")
data class FlyadealProperties(
    /**
     * Provider mode: "mock" or "real"
     * - mock: Uses MockNavitaireClient with simulated data
     * - real: Uses RealNavitaireClient connecting to actual Navitaire services
     */
    val provider: String = "mock",

    /**
     * Cache configuration settings
     */
    val cache: CacheProperties = CacheProperties(),

    /**
     * Mock provider simulation settings
     */
    val mock: MockProperties = MockProperties()
)

/**
 * Cache TTL configuration
 */
data class CacheProperties(
    /**
     * TTL for routes and stations cache in seconds (default: 24 hours)
     */
    val routesTtl: Long = 86400,

    /**
     * TTL for search results cache in seconds (default: 5 minutes)
     */
    val searchTtl: Long = 300
)

/**
 * Mock provider delay configuration for realistic latency simulation
 */
data class MockProperties(
    /**
     * Minimum delay in milliseconds for mock responses
     */
    val minDelay: Long = 500,

    /**
     * Maximum delay in milliseconds for mock responses
     */
    val maxDelay: Long = 1500
)
