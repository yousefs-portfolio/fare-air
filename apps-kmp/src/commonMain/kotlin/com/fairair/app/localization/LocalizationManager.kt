package com.fairair.app.localization

import androidx.compose.runtime.*
import androidx.compose.ui.unit.LayoutDirection

/**
 * Language enum representing supported languages.
 */
enum class AppLanguage(val code: String, val displayName: String, val layoutDirection: LayoutDirection) {
    ENGLISH("en", "English", LayoutDirection.Ltr),
    ARABIC("ar", "Arabic", LayoutDirection.Rtl);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * State holder for current language.
 */
class LocalizationState(
    initialLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var currentLanguage by mutableStateOf(initialLanguage)
        private set

    val strings: AppStrings
        get() = getStrings(currentLanguage.code)

    val layoutDirection: LayoutDirection
        get() = currentLanguage.layoutDirection

    val isRtl: Boolean
        get() = currentLanguage.layoutDirection == LayoutDirection.Rtl

    /**
     * Changes the current language.
     */
    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    /**
     * Changes the language by code.
     */
    fun setLanguage(code: String) {
        currentLanguage = AppLanguage.fromCode(code)
    }

    /**
     * Toggles between English and Arabic.
     */
    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ENGLISH) {
            AppLanguage.ARABIC
        } else {
            AppLanguage.ENGLISH
        }
    }
}

/**
 * CompositionLocal for providing localization state throughout the app.
 */
val LocalLocalization = compositionLocalOf { LocalizationState() }

/**
 * CompositionLocal for providing strings throughout the app.
 */
val LocalStrings = compositionLocalOf<AppStrings> { EnglishStrings }

/**
 * Provider composable that wraps content with localization support.
 */
@Composable
fun LocalizationProvider(
    localizationState: LocalizationState = remember { LocalizationState() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLocalization provides localizationState,
        LocalStrings provides localizationState.strings,
        androidx.compose.ui.platform.LocalLayoutDirection provides localizationState.layoutDirection
    ) {
        content()
    }
}

/**
 * Convenience extension to get strings from any composable.
 */
@Composable
fun strings(): AppStrings = LocalStrings.current

/**
 * Convenience extension to get localization state from any composable.
 */
@Composable
fun localization(): LocalizationState = LocalLocalization.current

/**
 * Remembers a LocalizationState instance that persists across recomposition.
 */
@Composable
fun rememberLocalizationState(
    initialLanguage: AppLanguage = AppLanguage.ENGLISH
): LocalizationState {
    return remember { LocalizationState(initialLanguage) }
}
