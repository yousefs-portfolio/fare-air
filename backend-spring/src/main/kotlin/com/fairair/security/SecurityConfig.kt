package com.fairair.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository

/**
 * Spring Security configuration for WebFlux.
 * Configures JWT authentication and authorization.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // Disable CSRF for stateless JWT authentication
            .csrf { it.disable() }
            
            // Disable form login and HTTP basic (we use JWT)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            
            // Use stateless session management
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            
            // Add JWT authentication filter
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            // Configure authorization
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints - no authentication required
                    .pathMatchers("/health", "/health/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api/v1/auth/**").permitAll()
                    .pathMatchers("/api/v1/config/**").permitAll()
                    
                    // Allow OPTIONS for CORS preflight
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    
                    // All other requests require authentication
                    .anyExchange().authenticated()
            }
            .build()
    }
}
