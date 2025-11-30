package com.flyadeal.config

import com.flyadeal.security.SecurityProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * CORS configuration for WebFlux.
 *
 * Configured via flyadeal.security.cors properties.
 * In production, only specific trusted origins should be allowed.
 */
@Configuration
@EnableWebFlux
class CorsConfig(
    private val securityProperties: SecurityProperties
) : WebFluxConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val corsConfig = securityProperties.cors
        
        registry.addMapping("/**")
            .allowedOrigins(*corsConfig.allowedOrigins.toTypedArray())
            .allowedMethods(*corsConfig.allowedMethods.toTypedArray())
            .allowedHeaders(
                "accept",
                "authorization",
                "content-type",
                "x-requested-with",
                "x-csrf-token"
            )
            .exposedHeaders(
                "Content-Disposition",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
            )
            .allowCredentials(true)
            .maxAge(corsConfig.maxAge)
    }
}
