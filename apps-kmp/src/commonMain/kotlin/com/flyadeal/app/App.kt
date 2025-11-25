package com.flyadeal.app

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.flyadeal.app.localization.AppLanguage
import com.flyadeal.app.localization.LocalizationProvider
import com.flyadeal.app.localization.LocalizationState
import com.flyadeal.app.persistence.LocalStorage
import com.flyadeal.app.ui.screens.search.SearchScreen
import com.flyadeal.app.ui.theme.FlyadealTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * Main application composable.
 * Sets up theming, DI context, localization, and navigation.
 */
@Composable
fun App() {
    KoinContext {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val localStorage = koinInject<LocalStorage>()

    // Create localization state and load saved language
    val localizationState = remember { LocalizationState() }

    // Load saved language preference on startup
    LaunchedEffect(Unit) {
        val savedLanguage = localStorage.getCurrentLanguage()
        localizationState.setLanguage(AppLanguage.fromCode(savedLanguage))
    }

    LocalizationProvider(localizationState = localizationState) {
        FlyadealTheme {
            Navigator(
                screen = SearchScreen(),
                onBackPressed = { currentScreen ->
                    // Allow back navigation for all screens except the first one
                    true
                }
            ) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
