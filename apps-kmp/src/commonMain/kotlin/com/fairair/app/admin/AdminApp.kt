package com.fairair.app.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.fairair.app.admin.screens.AdminLoginScreen
import com.fairair.app.ui.theme.FairairColors
import com.fairair.app.ui.theme.FairairTheme

/**
 * Root composable for the Admin portal.
 * Handles routing between login and dashboard.
 */
@Composable
fun AdminApp() {
    FairairTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FairairColors.Gray100
        ) {
            Navigator(AdminLoginScreen())
        }
    }
}
