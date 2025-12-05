package com.fairair.app.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.admin.api.AdminApiClient
import com.fairair.app.admin.api.AdminApiResult
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Admin login screen.
 */
class AdminLoginScreen : Screen {

    @Composable
    override fun Content() {
        val adminApiClient = koinInject<AdminApiClient>()
        val adminState = koinInject<AdminState>()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FairairColors.Gray100)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo/Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FairairColors.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Admin Icon
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin",
                            modifier = Modifier.size(64.dp),
                            tint = FairairColors.Purple
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Admin Portal",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = FairairColors.Purple
                        )

                        Text(
                            text = "fairair Content Management System",
                            fontSize = 14.sp,
                            color = FairairColors.Gray600,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FairairColors.Purple,
                                focusedLabelColor = FairairColors.Purple,
                                cursorColor = FairairColors.Purple
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Close else Icons.Default.Info,
                                        contentDescription = if (showPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FairairColors.Purple,
                                focusedLabelColor = FairairColors.Purple,
                                cursorColor = FairairColors.Purple
                            )
                        )

                        // Error Message
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage!!,
                                color = FairairColors.Error,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter email and password"
                                    return@Button
                                }

                                scope.launch {
                                    isLoading = true
                                    errorMessage = null

                                    when (val result = adminApiClient.login(email, password)) {
                                        is AdminApiResult.Success -> {
                                            val response = result.data
                                            if (response.success && response.admin != null && response.token != null) {
                                                adminState.setLoggedIn(response.admin, response.token)
                                                adminApiClient.setAuthToken(response.token)
                                                navigator.replace(AdminDashboardScreen())
                                            } else {
                                                errorMessage = response.message
                                            }
                                        }
                                        is AdminApiResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }

                                    isLoading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FairairColors.Purple,
                                contentColor = FairairColors.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = FairairColors.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Footer
                Text(
                    text = "© ${getCurrentYear()} fairair. All rights reserved.",
                    fontSize = 12.sp,
                    color = FairairColors.Gray500
                )
            }
        }
    }
}

private fun getCurrentYear(): Int {
    // In KMP, we'd use kotlinx-datetime, but for simplicity:
    return 2025
}
