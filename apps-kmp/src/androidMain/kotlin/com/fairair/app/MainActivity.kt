package com.fairair.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fairair.app.di.DefaultPlatformConfig
import com.fairair.app.di.PlatformConfig
import com.fairair.app.di.appModules
import com.fairair.app.persistence.initializeLocalStorage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Main activity for the Android application.
 * Initializes Koin DI and sets up the Compose UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize local storage with application context
        initializeLocalStorage(applicationContext)

        // Initialize Koin if not already started
        initKoin()

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set the Compose content
        setContent {
            App()
        }
    }

    private fun initKoin() {
        try {
            startKoin {
                androidLogger()
                androidContext(applicationContext)
                modules(appModules(androidModule))
            }
        } catch (e: IllegalStateException) {
            // Koin already started, ignore
        }
    }

    companion object {
        /**
         * Android-specific Koin module.
         * Provides platform-specific configuration.
         * Uses BuildConfig.IS_DEBUG which is set at build time for release/debug variants.
         */
        private val androidModule = module {
            single<PlatformConfig> {
                DefaultPlatformConfig(
                    // Use 10.0.2.2 for Android emulator to access host localhost
                    apiBaseUrl = "http://10.0.2.2:8080",
                    isDebug = BuildConfig.IS_DEBUG
                )
            }
        }
    }
}
