package com.fairair.app.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Standard pointer icon for clickable elements.
 */
val PointerIconHand = PointerIcon.Hand

/**
 * Modifier extension that makes an element clickable with a pointer cursor.
 */
fun Modifier.clickableWithCursor(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this
    .pointerHoverIcon(PointerIcon.Hand)
    .clickable(enabled = enabled, onClick = onClick)
