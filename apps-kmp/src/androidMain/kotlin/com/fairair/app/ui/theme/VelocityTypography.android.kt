package com.fairair.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.fairair.apps_kmp.generated.resources.*
import org.jetbrains.compose.resources.Font

/**
 * Android implementation of SpaceGroteskFontFamily.
 * Uses compose-resources Font() which loads synchronously on Android.
 */
@Composable
actual fun SpaceGroteskFontFamily(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk_light, FontWeight.Light),
    Font(Res.font.space_grotesk_regular, FontWeight.Normal),
    Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(Res.font.space_grotesk_bold, FontWeight.Bold)
)

/**
 * Android implementation of NotoKufiArabicFontFamily.
 * Uses compose-resources Font() which loads synchronously on Android.
 */
@Composable
actual fun NotoKufiArabicFontFamily(): FontFamily = FontFamily(
    Font(Res.font.noto_kufi_arabic_light, FontWeight.Light),
    Font(Res.font.noto_kufi_arabic_regular, FontWeight.Normal),
    Font(Res.font.noto_kufi_arabic_semibold, FontWeight.SemiBold),
    Font(Res.font.noto_kufi_arabic_bold, FontWeight.Bold)
)
