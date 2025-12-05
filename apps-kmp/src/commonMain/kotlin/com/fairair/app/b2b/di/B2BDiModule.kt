package com.fairair.app.b2b.di

import com.fairair.app.b2b.api.B2BApiClient
import com.fairair.app.b2b.state.B2BState
import org.koin.dsl.module

/**
 * Koin DI module for B2B Agency Portal.
 * Provides singleton instances of B2BApiClient and B2BState.
 */
val b2bModule = module {
    // API Client - singleton for the entire B2B portal session
    single { B2BApiClient() }

    // B2B State - centralized state management, depends on ApiClient
    single { B2BState(get()) }
}
