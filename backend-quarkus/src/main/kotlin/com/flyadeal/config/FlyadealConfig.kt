package com.flyadeal.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

/**
 * Configuration properties for the flyadeal backend.
 * Mapped from application.properties/yaml using the "flyadeal" prefix.
 */
@ConfigMapping(prefix = "flyadeal")
interface FlyadealConfig {

    /**
     * Provider type: "mock" or "real"
     * Controls which Navitaire client implementation is used.
     */
    @WithName("provider")
    @WithDefault("mock")
    fun provider(): String

    /**
     * Cache configuration settings.
     */
    fun cache(): CacheConfig

    /**
     * Mock provider configuration settings.
     */
    fun mock(): MockConfig

    /**
     * Returns true if using mock provider.
     */
    fun isMockProvider(): Boolean = provider() == "mock"
}

/**
 * Cache TTL configuration.
 */
interface CacheConfig {
    /**
     * TTL for routes/stations cache in seconds.
     * Default: 24 hours (86400 seconds)
     */
    @WithName("routes-ttl")
    @WithDefault("86400")
    fun routesTtl(): Long

    /**
     * TTL for flight search results cache in seconds.
     * Default: 5 minutes (300 seconds)
     */
    @WithName("search-ttl")
    @WithDefault("300")
    fun searchTtl(): Long
}

/**
 * Mock provider configuration for simulating realistic API behavior.
 */
interface MockConfig {
    /**
     * Minimum delay in milliseconds for mock responses.
     * Default: 500ms
     */
    @WithName("min-delay")
    @WithDefault("500")
    fun minDelay(): Long

    /**
     * Maximum delay in milliseconds for mock responses.
     * Default: 1500ms
     */
    @WithName("max-delay")
    @WithDefault("1500")
    fun maxDelay(): Long
}
