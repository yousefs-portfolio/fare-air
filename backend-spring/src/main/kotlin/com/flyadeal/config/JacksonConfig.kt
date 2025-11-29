package com.flyadeal.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Jackson ObjectMapper configuration for JSON serialization.
 *
 * Configures Jackson to match the Quarkus serialization behavior:
 * - ISO-8601 date format (not timestamps)
 * - Ignore unknown properties during deserialization
 * - Kotlin module for data class support
 */
@Configuration
class JacksonConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // Register modules
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())

            // Match Quarkus serialization config
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            // Include non-null values only
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}
