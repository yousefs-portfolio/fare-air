package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.b2b.api.AgencyRegistrationRequest
import com.fairair.app.b2b.state.B2BState
import org.koin.compose.koinInject

/**
 * B2B Agency Login Screen.
 * Provides login functionality for travel agencies with registration option.
 */
class B2BLoginScreen : Screen {

    @Composable
    override fun Content() {
        val b2bState = koinInject<B2BState>()
        val navigator = LocalNavigator.currentOrThrow

        val isLoggedIn by b2bState.isLoggedIn.collectAsState()

        // Navigate to dashboard when logged in
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                navigator.replaceAll(B2BDashboardScreen())
            }
        }

        B2BLoginContent(b2bState = b2bState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun B2BLoginContent(b2bState: B2BState) {
    var showRegistration by remember { mutableStateOf(false) }

    // B2B brand colors
    val primaryColor = Color(0xFF1E40AF) // Deep blue
    val accentColor = Color(0xFF06B6D4) // Cyan
    val backgroundColor = Color(0xFF0F172A) // Dark navy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Branding
            Text(
                text = "FairAir",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = "B2B Travel Partner Portal",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Login/Register Card
            Card(
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                )
            ) {
                if (showRegistration) {
                    RegistrationForm(
                        b2bState = b2bState,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        onBackToLogin = { showRegistration = false }
                    )
                } else {
                    LoginForm(
                        b2bState = b2bState,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        onRegisterClick = { showRegistration = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "Need help? Contact B2B Support",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LoginForm(
    b2bState: B2BState,
    primaryColor: Color,
    accentColor: Color,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isLoggingIn by b2bState.isLoggingIn.collectAsState()
    val authError by b2bState.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign In",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Error message
        authError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error,
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = accentColor)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = accentColor,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                cursorColor = accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = accentColor)
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = accentColor,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                cursorColor = accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Login button
        Button(
            onClick = {
                b2bState.clearAuthError()
                b2bState.login(email.trim(), password)
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Forgot password link
        TextButton(onClick = { /* TODO: Forgot password */ }) {
            Text(
                text = "Forgot Password?",
                color = accentColor.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = Color.White.copy(alpha = 0.2f)
        )

        // Register link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            TextButton(onClick = onRegisterClick) {
                Text(
                    text = "Register Agency",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    b2bState: B2BState,
    primaryColor: Color,
    accentColor: Color,
    onBackToLogin: () -> Unit
) {
    var agencyName by remember { mutableStateOf("") }
    var iataCode by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var registrationSuccess by remember { mutableStateOf(false) }

    val isLoggingIn by b2bState.isLoggingIn.collectAsState()
    val authError by b2bState.authError.collectAsState()

    if (registrationSuccess) {
        // Show success message
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Registration Submitted!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your agency registration has been submitted for approval. You will receive an email once your account is activated.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onBackToLogin,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Back to Login")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register Your Agency",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Join our B2B partner network",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Error message
            authError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Agency Information Section
            Text(
                text = "Agency Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegistrationTextField(
                    value = agencyName,
                    onValueChange = { agencyName = it },
                    label = "Agency Name",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                RegistrationTextField(
                    value = iataCode,
                    onValueChange = { iataCode = it.take(10) },
                    label = "IATA Code",
                    accentColor = accentColor,
                    modifier = Modifier.width(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RegistrationTextField(
                value = licenseNumber,
                onValueChange = { licenseNumber = it },
                label = "Business License Number",
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Contact Information Section
            Text(
                text = "Contact Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            RegistrationTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = "Contact Person Name",
                leadingIcon = Icons.Default.Person,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegistrationTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                RegistrationTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    keyboardType = KeyboardType.Phone,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RegistrationTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address",
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegistrationTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = "City",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                RegistrationTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = "Country",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Password Section
            Text(
                text = "Create Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            RegistrationTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = !showPassword,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            RegistrationTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = !showPassword,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )

            // Password validation
            if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = "Passwords do not match",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register button
            val isFormValid = agencyName.isNotBlank() &&
                    iataCode.isNotBlank() &&
                    licenseNumber.isNotBlank() &&
                    contactName.isNotBlank() &&
                    email.isNotBlank() &&
                    phone.isNotBlank() &&
                    address.isNotBlank() &&
                    city.isNotBlank() &&
                    country.isNotBlank() &&
                    password.isNotBlank() &&
                    password == confirmPassword &&
                    password.length >= 8

            Button(
                onClick = {
                    b2bState.clearAuthError()
                    b2bState.registerAgency(
                        AgencyRegistrationRequest(
                            agencyName = agencyName.trim(),
                            iataCode = iataCode.trim(),
                            businessLicenseNumber = licenseNumber.trim(),
                            contactPersonName = contactName.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            country = country.trim(),
                            password = password
                        ),
                        onSuccess = { registrationSuccess = true }
                    )
                },
                enabled = isFormValid && !isLoggingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Registration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isFormValid && password.isNotBlank()) {
                Text(
                    text = "Password must be at least 8 characters",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Back to login link
            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Already have an account? Sign In",
                    color = accentColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    accentColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp)) }
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
            cursorColor = accentColor,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        modifier = modifier
    )
}
