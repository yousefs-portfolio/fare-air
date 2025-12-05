package com.fairair.app.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.navigator.Navigator
import com.fairair.app.b2b.di.b2bModule
import com.fairair.app.b2b.screens.B2BDashboardScreen
import com.fairair.app.b2b.screens.B2BLoginScreen
import com.fairair.app.b2b.state.B2BState
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

/**
 * B2B Agency Portal main entry point.
 * Provides a complete portal for travel agencies to:
 * - Search and book flights for clients
 * - Manage agency bookings
 * - Submit group and charter requests
 * - View agency profile and statistics
 */
@Composable
fun B2BApp() {
    KoinApplication(application = {
        modules(b2bModule)
    }) {
        val b2bState: B2BState = koinInject()

        MaterialTheme(
            colorScheme = B2BColorScheme
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(B2BColors.Background)
            ) {
                B2BPortalContent(b2bState)
            }
        }
    }
}

/**
 * B2B Portal content with authentication-aware navigation.
 */
@Composable
private fun B2BPortalContent(b2bState: B2BState) {
    val isLoggedIn by b2bState.isLoggedIn.collectAsState()

    if (isLoggedIn) {
        Navigator(B2BDashboardScreen())
    } else {
        Navigator(B2BLoginScreen())
    }
}

/**
 * B2B Portal color palette - purple/violet theme for agency branding.
 */
object B2BColors {
    val Primary = Color(0xFF8B5CF6)      // Purple accent
    val PrimaryDark = Color(0xFF7C3AED)
    val Secondary = Color(0xFF06B6D4)    // Cyan for accents
    val Background = Color(0xFF0F172A)   // Deep navy background
    val Surface = Color(0xFF1E293B)      // Slate surface
    val SurfaceVariant = Color(0xFF334155)
    val OnBackground = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFFE2E8F0)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
}

/**
 * Material3 dark color scheme for B2B portal.
 */
private val B2BColorScheme = darkColorScheme(
    primary = B2BColors.Primary,
    onPrimary = Color.White,
    secondary = B2BColors.Secondary,
    onSecondary = Color.White,
    background = B2BColors.Background,
    onBackground = B2BColors.OnBackground,
    surface = B2BColors.Surface,
    onSurface = B2BColors.OnSurface,
    surfaceVariant = B2BColors.SurfaceVariant,
    error = B2BColors.Error,
    onError = Color.White
)
