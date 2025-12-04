package com.fairair.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

/**
 * Database configuration for R2DBC.
 * 
 * Current setup uses H2 in file-based mode for persistent storage.
 * 
 * To switch to PostgreSQL:
 * 1. Replace r2dbc-h2 dependency with r2dbc-postgresql in build.gradle.kts
 * 2. Update application.yml with PostgreSQL connection URL:
 *    spring.r2dbc.url: r2dbc:postgresql://host:5432/database
 *    spring.r2dbc.username: user
 *    spring.r2dbc.password: pass
 * 3. Update schema.sql if needed (syntax should be compatible)
 */
@Configuration
@EnableR2dbcAuditing
class DatabaseConfig {

    /**
     * Initializes the database schema on startup.
     * Runs the schema.sql file to create tables if they don't exist.
     */
    @Bean
    fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)
        
        val populator = CompositeDatabasePopulator()
        populator.addPopulators(
            ResourceDatabasePopulator(ClassPathResource("schema.sql")),
            ResourceDatabasePopulator(ClassPathResource("data.sql"))
        )
        initializer.setDatabasePopulator(populator)
        
        return initializer
    }
}
