package com.fairair.app

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.fairair.app.localization.AppLanguage
import com.fairair.app.localization.LocalizationProvider
import com.fairair.app.localization.LocalizationState
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.ui.screens.search.SearchScreen
import com.fairair.app.ui.theme.FairairTheme
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
        FairairTheme {
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
