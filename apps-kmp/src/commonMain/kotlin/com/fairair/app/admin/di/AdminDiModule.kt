package com.fairair.app.admin.di

import com.fairair.app.admin.api.AdminApiClient
import com.fairair.app.admin.state.AdminState
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin dependency injection module for the Admin portal.
 */
val adminModule = module {
    // HTTP Client for Admin API
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            
            defaultRequest {
                headers.append("Content-Type", "application/json")
                headers.append("Accept", "application/json")
            }
        }
    }
    
    // Admin API Client
    single {
        AdminApiClient(
            baseUrl = getProperty("api.baseUrl", "http://localhost:8080"),
            httpClient = get()
        )
    }
    
    // Admin State (singleton for session)
    singleOf(::AdminState)
}

/**
 * Properties for admin configuration.
 */
object AdminConfig {
    var baseUrl: String = "http://localhost:8080"
    var tokenStorageKey: String = "admin_auth_token"
    
    fun configure(baseUrl: String) {
        this.baseUrl = baseUrl
    }
}
