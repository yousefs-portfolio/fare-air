package com.fairair.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Space Grotesk font family for English/Latin text.
 * Platform-specific implementation:
 * - Android/iOS: Uses compose-resources Font()
 * - WasmJs: Uses pre-loaded fonts from main.kt
 */
@Composable
expect fun SpaceGroteskFontFamily(): FontFamily

/**
 * Noto Kufi Arabic font family for Arabic/RTL text.
 * Platform-specific implementation:
 * - Android/iOS: Uses compose-resources Font()
 * - WasmJs: Uses pre-loaded fonts from main.kt
 */
@Composable
expect fun NotoKufiArabicFontFamily(): FontFamily

/**
 * Velocity typography definitions for English/Latin text.
 */
@Immutable
data class VelocityTypography(
    /**
     * Hero title - 64sp Light for main headlines
     */
    val heroTitle: TextStyle,

    /**
     * Sentence builder text - 40sp Light for natural language interface
     */
    val sentenceBuilder: TextStyle,

    /**
     * Magic input field text - 40sp Bold for selected values in sentence
     */
    val magicInput: TextStyle,

    /**
     * Flight path display - 48sp Bold for route codes in results
     */
    val flightPath: TextStyle,

    /**
     * Large time display - 32sp Bold for departure/arrival times
     */
    val timeBig: TextStyle,

    /**
     * Price display - 28sp Bold for fare prices
     */
    val priceDisplay: TextStyle,

    /**
     * Small label - 10sp Bold uppercase for fare family labels
     */
    val labelSmall: TextStyle,

    /**
     * Body text - 16sp Regular for general content
     */
    val body: TextStyle,

    /**
     * Button text - 16sp SemiBold for buttons
     */
    val button: TextStyle,

    /**
     * Duration label - 14sp Regular for flight duration
     */
    val duration: TextStyle
)

/**
 * Creates VelocityTypography with the specified font family.
 */
@Composable
fun createVelocityTypography(fontFamily: FontFamily): VelocityTypography = VelocityTypography(
    heroTitle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        color = VelocityColors.TextMain
    ),
    sentenceBuilder = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 52.sp,
        color = VelocityColors.TextMain
    ),
    magicInput = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 52.sp,
        color = VelocityColors.Accent
    ),
    flightPath = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        color = VelocityColors.TextMain
    ),
    timeBig = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = VelocityColors.TextMain
    ),
    priceDisplay = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = VelocityColors.TextMain
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
        color = VelocityColors.TextMuted
    ),
    body = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = VelocityColors.TextMain
    ),
    button = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = VelocityColors.BackgroundDeep
    ),
    duration = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = VelocityColors.TextMuted
    )
)

/**
 * Creates VelocityTypography for Arabic with adjusted sizes.
 */
@Composable
fun createArabicTypography(fontFamily: FontFamily): VelocityTypography = VelocityTypography(
    heroTitle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp, // Reduced for Arabic
        lineHeight = 56.sp,
        color = VelocityColors.TextMain
    ),
    sentenceBuilder = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp, // Reduced for Arabic
        lineHeight = 44.sp,
        color = VelocityColors.TextMain
    ),
    magicInput = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp, // Reduced for Arabic
        lineHeight = 44.sp,
        color = VelocityColors.Accent
    ),
    flightPath = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        color = VelocityColors.TextMain
    ),
    timeBig = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = VelocityColors.TextMain
    ),
    priceDisplay = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = VelocityColors.TextMain
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = VelocityColors.TextMuted
    ),
    body = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = VelocityColors.TextMain
    ),
    button = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = VelocityColors.BackgroundDeep
    ),
    duration = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = VelocityColors.TextMuted
    )
)

/**
 * CompositionLocal for accessing VelocityTypography in the composition tree.
 */
val LocalVelocityTypography = staticCompositionLocalOf<VelocityTypography> {
    error("VelocityTypography not provided. Wrap your composable in VelocityTheme.")
}
