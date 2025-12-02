package com.fairair.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * fairair brand colors
 */
object FairairColors {
    // Primary - fairair purple
    val Purple = Color(0xFF6B2D83)
    val PurpleDark = Color(0xFF4A1F5C)
    val PurpleLight = Color(0xFF9B5FB5)

    // Secondary - fairair yellow/gold
    val Yellow = Color(0xFFFFC629)
    val YellowDark = Color(0xFFE5A800)
    val YellowLight = Color(0xFFFFD966)

    // Semantic colors
    val Success = Color(0xFF28A745)
    val Error = Color(0xFFDC3545)
    val Warning = Color(0xFFFFC107)
    val Info = Color(0xFF17A2B8)

    // Neutrals
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Gray50 = Color(0xFFFAFAFA)
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEEEEEE)
    val Gray300 = Color(0xFFE0E0E0)
    val Gray400 = Color(0xFFBDBDBD)
    val Gray500 = Color(0xFF9E9E9E)
    val Gray600 = Color(0xFF757575)
    val Gray700 = Color(0xFF616161)
    val Gray800 = Color(0xFF424242)
    val Gray900 = Color(0xFF212121)
}

/**
 * Light color scheme for fairair app
 */
private val LightColorScheme = lightColorScheme(
    primary = FairairColors.Purple,
    onPrimary = FairairColors.White,
    primaryContainer = FairairColors.PurpleLight,
    onPrimaryContainer = FairairColors.White,
    secondary = FairairColors.Yellow,
    onSecondary = FairairColors.Black,
    secondaryContainer = FairairColors.YellowLight,
    onSecondaryContainer = FairairColors.Black,
    tertiary = FairairColors.Info,
    onTertiary = FairairColors.White,
    background = FairairColors.White,
    onBackground = FairairColors.Gray900,
    surface = FairairColors.White,
    onSurface = FairairColors.Gray900,
    surfaceVariant = FairairColors.Gray100,
    onSurfaceVariant = FairairColors.Gray700,
    error = FairairColors.Error,
    onError = FairairColors.White,
    outline = FairairColors.Gray400
)

/**
 * Dark color scheme for fairair app
 */
private val DarkColorScheme = darkColorScheme(
    primary = FairairColors.PurpleLight,
    onPrimary = FairairColors.Black,
    primaryContainer = FairairColors.Purple,
    onPrimaryContainer = FairairColors.White,
    secondary = FairairColors.Yellow,
    onSecondary = FairairColors.Black,
    secondaryContainer = FairairColors.YellowDark,
    onSecondaryContainer = FairairColors.White,
    tertiary = FairairColors.Info,
    onTertiary = FairairColors.White,
    background = FairairColors.Gray900,
    onBackground = FairairColors.White,
    surface = FairairColors.Gray800,
    onSurface = FairairColors.White,
    surfaceVariant = FairairColors.Gray700,
    onSurfaceVariant = FairairColors.Gray300,
    error = FairairColors.Error,
    onError = FairairColors.White,
    outline = FairairColors.Gray600
)

/**
 * Custom spacing values for consistent UI
 */
data class FairairSpacing(
    val xs: Int = 4,
    val sm: Int = 8,
    val md: Int = 16,
    val lg: Int = 24,
    val xl: Int = 32,
    val xxl: Int = 48
)

/**
 * Custom typography adjustments
 */
val FairairTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

val LocalSpacing = staticCompositionLocalOf { FairairSpacing() }

/**
 * fairair Material 3 theme wrapper
 */
@Composable
fun FairairTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSpacing provides FairairSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FairairTypography,
            content = content
        )
    }
}

/**
 * Extension to access spacing from any composable
 */
val MaterialTheme.spacing: FairairSpacing
    @Composable
    get() = LocalSpacing.current
