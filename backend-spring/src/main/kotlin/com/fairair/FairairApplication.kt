package com.fairair

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Main entry point for the fairair BFF Spring Boot application.
 *
 * This application serves as the Backend for Frontend (BFF) for the fairair
 * airline booking platform, providing REST APIs for the Compose Multiplatform
 * frontend applications (Android, iOS, Web).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class FairairApplication

fun main(args: Array<String>) {
    runApplication<FairairApplication>(*args)
}
