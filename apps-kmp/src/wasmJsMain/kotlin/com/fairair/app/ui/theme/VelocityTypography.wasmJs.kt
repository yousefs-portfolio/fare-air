package com.fairair.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.fairair.app.ArabicFontFamily
import com.fairair.app.LatinFontFamily

/**
 * WasmJs implementation of SpaceGroteskFontFamily.
 * Uses pre-loaded fonts from main.kt to ensure they are fully loaded
 * before rendering (avoiding async loading issues).
 */
@Composable
actual fun SpaceGroteskFontFamily(): FontFamily = LatinFontFamily

/**
 * WasmJs implementation of NotoKufiArabicFontFamily.
 * Uses pre-loaded fonts from main.kt to ensure Arabic text renders correctly.
 */
@Composable
actual fun NotoKufiArabicFontFamily(): FontFamily = ArabicFontFamily
