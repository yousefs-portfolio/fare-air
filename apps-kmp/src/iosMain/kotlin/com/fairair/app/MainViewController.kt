package com.fairair.app

import androidx.compose.ui.window.ComposeUIViewController
import com.fairair.app.di.DefaultPlatformConfig
import com.fairair.app.di.PlatformConfig
import com.fairair.app.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

/**
 * Determines if we're running in debug mode.
 * On iOS, we check the isDebugBinary property.
 */
@OptIn(ExperimentalNativeApi::class)
private val isDebugMode: Boolean = Platform.isDebugBinary

/**
 * iOS-specific Koin module.
 * Uses Platform.isDebugBinary to determine debug mode at runtime.
 */
private val iosModule = module {
    single<PlatformConfig> {
        DefaultPlatformConfig(
            apiBaseUrl = "http://localhost:8080",
            isDebug = isDebugMode
        )
    }
}

/**
 * Initializes Koin for iOS.
 * Should be called from iOS AppDelegate or similar.
 */
fun initKoin() {
    startKoin {
        modules(appModules(iosModule))
    }
}

/**
 * Creates the main UIViewController for iOS.
 * This is called from Swift code.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
