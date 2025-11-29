package com.flyadeal.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * FairAir UI design system colors.
 * Teal primary with coral accent on a dark slate background for a modern, trustworthy experience.
 */
@Immutable
object VelocityColors {
    /**
     * Primary brand color - teal (#0D9488)
     * Used for buttons, headers, and primary actions
     */
    val Primary = Color(0xFF0D9488)

    /**
     * Primary app background - dark slate (#1E293B)
     */
    val BackgroundDeep = Color(0xFF1E293B)

    /**
     * Mid-tone background - medium slate (#334155)
     * Used for cards and elevated surfaces
     */
    val BackgroundMid = Color(0xFF334155)

    /**
     * Accent color for CTAs, highlights, and interactive elements - coral (#F97316)
     */
    val Accent = Color(0xFFF97316)

    /**
     * Glassmorphism card background - semi-transparent dark slate
     */
    val GlassBg = Color(0xFF1E293B).copy(alpha = 0.5f)

    /**
     * Glassmorphism card hover/active state
     */
    val GlassHover = Color(0xFF334155).copy(alpha = 0.6f)

    /**
     * Glassmorphism card border - subtle white
     */
    val GlassBorder = Color.White.copy(alpha = 0.1f)

    /**
     * Primary text color - pure white
     */
    val TextMain = Color.White

    /**
     * Secondary/muted text color - slate gray (#94A3B8)
     */
    val TextMuted = Color(0xFF94A3B8)

    /**
     * Glow effect for launch button - coral accent with reduced alpha
     */
    val NeonGlow = Color(0xFFF97316).copy(alpha = 0.3f)

    /**
     * Disabled state color
     */
    val Disabled = Color.White.copy(alpha = 0.3f)

    /**
     * Error state color - red (#EF4444)
     */
    val Error = Color(0xFFEF4444)

    /**
     * Success state color - green (#22C55E)
     */
    val Success = Color(0xFF22C55E)

    /**
     * Warning state color - amber (#F59E0B)
     */
    val Warning = Color(0xFFF59E0B)

    /**
     * Background gradient start (top) - darker teal (#0F766E)
     */
    val GradientStart = Color(0xFF0F766E)

    /**
     * Background gradient end (bottom) - dark slate (#1E293B)
     */
    val GradientEnd = Color(0xFF1E293B)
}

/**
 * CompositionLocal for accessing VelocityColors in the composition tree.
 */
val LocalVelocityColors = staticCompositionLocalOf { VelocityColors }
