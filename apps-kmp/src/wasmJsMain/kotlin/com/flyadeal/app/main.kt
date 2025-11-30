package com.flyadeal.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.flyadeal.app.di.DefaultPlatformConfig
import com.flyadeal.app.di.PlatformConfig
import com.flyadeal.app.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Determines if we're running in debug mode for WasmJs.
 * For web builds, debug mode is determined by the build configuration.
 * In production, this should be false. The value is set at build time.
 * 
 * TODO: Use webpack DefinePlugin or similar to inject this at build time.
 * For now, we default to false (production) as the safe default.
 */
private val isDebugMode: Boolean = false

/**
 * WasmJs-specific Koin module.
 */
private val wasmJsModule = module {
    single<PlatformConfig> {
        DefaultPlatformConfig(
            apiBaseUrl = "http://localhost:8080",
            isDebug = isDebugMode
        )
    }
}

/**
 * Main entry point for the WasmJs application.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin
    startKoin {
        modules(appModules(wasmJsModule))
    }

    // Start the Compose application
    CanvasBasedWindow(
        canvasElementId = "ComposeTarget",
        title = "FairAir"
    ) {
        App()
    }
}
