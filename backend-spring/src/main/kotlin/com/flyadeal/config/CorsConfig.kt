package com.flyadeal.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * CORS configuration for WebFlux.
 *
 * Matches the Quarkus CORS configuration to allow frontend access from:
 * - localhost:8081 (Android emulator)
 * - localhost:3000 (Web development server)
 * - 127.0.0.1:8081 (Alternative Android emulator address)
 */
@Configuration
@EnableWebFlux
class CorsConfig : WebFluxConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:8081",
                "http://localhost:3000",
                "http://127.0.0.1:8081"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders(
                "accept",
                "authorization",
                "content-type",
                "x-requested-with"
            )
            .exposedHeaders("Content-Disposition")
            .maxAge(86400) // 24 hours in seconds
    }
}
