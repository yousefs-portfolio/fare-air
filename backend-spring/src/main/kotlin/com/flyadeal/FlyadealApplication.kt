package com.flyadeal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Main entry point for the flyadeal BFF Spring Boot application.
 *
 * This application serves as the Backend for Frontend (BFF) for the flyadeal
 * airline booking platform, providing REST APIs for the Compose Multiplatform
 * frontend applications (Android, iOS, Web).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class FlyadealApplication

fun main(args: Array<String>) {
    runApplication<FlyadealApplication>(*args)
}
