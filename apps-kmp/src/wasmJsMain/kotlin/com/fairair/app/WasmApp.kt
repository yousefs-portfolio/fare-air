package com.fairair.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fairair.app.api.FairairApiClient
import com.fairair.app.localization.AppLanguage
import com.fairair.app.localization.LocalizationProvider
import com.fairair.app.localization.LocalizationState
import com.fairair.app.localization.rememberLocalizationState
import com.fairair.app.state.BookingFlowState
import com.fairair.app.ui.screens.results.*
import com.fairair.app.ui.screens.search.VelocitySearchScreen
import com.fairair.app.ui.screens.search.VelocitySearchScreenError
import com.fairair.app.ui.screens.search.VelocitySearchScreenLoading
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme
import com.fairair.app.ui.theme.VelocityThemeWithBackground
import com.fairair.app.viewmodel.*
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * Wasm-specific application entry point that bypasses Voyager Navigator.
 *
 * Voyager's Navigator and Screen classes cause JsException in Wasm due to
 * internal serialization/reflection mechanisms that aren't compatible with
 * the Wasm runtime. This implementation uses direct composable rendering
 * with state-based navigation instead.
 *
 * This provides a complete booking flow:
 * Search -> Results -> Passengers -> Payment -> Confirmation
 */
@Composable
fun WasmApp() {
    KoinContext {
        WasmAppContent()
    }
}

/**
 * Navigation state for Wasm app.
 * Since we can't use Voyager, we use simple enum-based navigation.
 */
private enum class WasmScreen {
    SEARCH,
    RESULTS,
    PASSENGERS,
    PAYMENT,
    CONFIRMATION,
    SETTINGS,
    SAVED_BOOKINGS
}

@Composable
private fun WasmAppContent() {
    val apiClient = koinInject<FairairApiClient>()
    val bookingFlowState = koinInject<BookingFlowState>()
    val localizationState = rememberLocalizationState()
    val scope = rememberCoroutineScope()

    // Navigation state
    var currentScreen by remember { mutableStateOf(WasmScreen.SEARCH) }

    // ViewModels - created once and reused
    val searchViewModel = remember {
        WasmSearchViewModel(apiClient, bookingFlowState, scope)
    }
    val bookingViewModel = remember {
        WasmBookingViewModel(apiClient, bookingFlowState, scope)
    }

    // Wrap in localization provider
    LocalizationProvider(localizationState) {
        when (currentScreen) {
            WasmScreen.SEARCH -> {
                WasmSearchScreenContainer(
                    viewModel = searchViewModel,
                    localizationState = localizationState,
                    onNavigateToResults = {
                        bookingViewModel.initializeResults()
                        currentScreen = WasmScreen.RESULTS
                    },
                    onNavigateToSettings = { currentScreen = WasmScreen.SETTINGS },
                    onNavigateToSavedBookings = { currentScreen = WasmScreen.SAVED_BOOKINGS }
                )
            }
            WasmScreen.RESULTS -> {
                WasmResultsScreenContainer(
                    viewModel = bookingViewModel,
                    localizationState = localizationState,
                    onBack = { currentScreen = WasmScreen.SEARCH },
                    onContinue = {
                        bookingViewModel.initializePassengerForms()
                        currentScreen = WasmScreen.PASSENGERS
                    }
                )
            }
            WasmScreen.PASSENGERS -> {
                WasmPassengerScreenContainer(
                    viewModel = bookingViewModel,
                    localizationState = localizationState,
                    onBack = { currentScreen = WasmScreen.RESULTS },
                    onContinue = {
                        bookingViewModel.initializePayment()
                        currentScreen = WasmScreen.PAYMENT
                    }
                )
            }
            WasmScreen.PAYMENT -> {
                WasmPaymentScreenContainer(
                    viewModel = bookingViewModel,
                    localizationState = localizationState,
                    onBack = { currentScreen = WasmScreen.PASSENGERS },
                    onSuccess = {
                        bookingViewModel.initializeConfirmation()
                        currentScreen = WasmScreen.CONFIRMATION
                    }
                )
            }
            WasmScreen.CONFIRMATION -> {
                WasmConfirmationScreenContainer(
                    viewModel = bookingViewModel,
                    localizationState = localizationState,
                    onNewBooking = {
                        bookingViewModel.resetForNewBooking()
                        searchViewModel.retry()
                        currentScreen = WasmScreen.SEARCH
                    }
                )
            }
            WasmScreen.SETTINGS -> {
                WasmSettingsScreen(
                    localizationState = localizationState,
                    onBack = { currentScreen = WasmScreen.SEARCH }
                )
            }
            WasmScreen.SAVED_BOOKINGS -> {
                WasmSavedBookingsPlaceholder(
                    onBack = { currentScreen = WasmScreen.SEARCH }
                )
            }
        }
    }
}

@Composable
private fun WasmSearchScreenContainer(
    viewModel: WasmSearchViewModel,
    localizationState: LocalizationState,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedBookings: () -> Unit
) {
    val velocityState by viewModel.velocityState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    when {
        velocityState.isLoading -> {
            VelocitySearchScreenLoading(isRtl = isRtl)
        }
        velocityState.error != null && velocityState.availableOrigins.isEmpty() -> {
            VelocitySearchScreenError(
                message = velocityState.error ?: "An error occurred",
                onRetry = viewModel::retry,
                isRtl = isRtl,
                strings = strings
            )
        }
        else -> {
            VelocitySearchScreen(
                state = velocityState,
                strings = strings,
                isRtl = isRtl,
                onOriginSelect = viewModel::selectVelocityOrigin,
                onDestinationSelect = viewModel::selectVelocityDestination,
                onDateSelect = viewModel::selectVelocityDate,
                onPassengerSelect = viewModel::setVelocityPassengerCount,
                onFieldActivate = viewModel::setActiveField,
                onSearch = {
                    viewModel.searchFromVelocity {
                        onNavigateToResults()
                    }
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSavedBookings = onNavigateToSavedBookings
            )
        }
    }
}

@Composable
private fun WasmResultsScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.resultsState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    val resultsState = VelocityResultsState(
        isVisible = true,
        isLoading = state.isLoading,
        flights = state.velocityFlights,
        expandedFlightId = state.expandedFlightId,
        selectedFare = null,
        error = state.error
    )

    VelocityThemeWithBackground(isRtl = isRtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Back button at top
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VelocityColors.TextMain
                )
            }

            // Results overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                VelocityResultsOverlay(
                    state = resultsState,
                    originCode = state.originCode,
                    destinationCode = state.destinationCode,
                    formattedDate = state.departureDate,
                    onFlightClick = { velocityFlight ->
                        val flight = state.flights.find { it.flightNumber == velocityFlight.id }
                        if (flight != null) {
                            viewModel.selectFlight(flight)
                        }
                    },
                    onFareSelect = { fareFamily ->
                        if (viewModel.selectFare(fareFamily)) {
                            onContinue()
                        }
                    },
                    onClose = onBack,
                    onRetry = { /* reload results */ },
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun WasmPassengerScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.passengerState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl
    val currentPassenger = state.currentPassenger

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header with glassmorphic style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMain
                    )
                }

                // Title centered
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Passengers",
                        style = VelocityTheme.typography.timeBig,
                        color = VelocityColors.TextMain
                    )
                    if (state.passengers.isNotEmpty()) {
                        Text(
                            text = "${state.currentIndex + 1} of ${state.passengers.size}",
                            style = VelocityTheme.typography.duration,
                            color = VelocityColors.TextMuted
                        )
                    }
                }
            }

            // Progress indicator - dots style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.passengers.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == state.currentIndex) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= state.currentIndex) VelocityColors.Accent
                                else VelocityColors.GlassBorder
                            )
                    )
                    if (index < state.passengers.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (index < state.currentIndex) VelocityColors.Accent
                                    else VelocityColors.GlassBorder
                                )
                        )
                    }
                }
            }

            if (currentPassenger != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Passenger type badge
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = VelocityColors.Accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (currentPassenger.type) {
                                            "ADULT" -> Icons.Default.Person
                                            "CHILD" -> Icons.Default.Face
                                            else -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = VelocityColors.Accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = currentPassenger.label,
                                        style = VelocityTheme.typography.button,
                                        color = VelocityColors.Accent
                                    )
                                }
                            }
                        }
                    }

                    // Personal Details Card
                    item {
                        VelocityFormCard(title = "Personal Details") {
                            // Title selector
                            VelocityChipSelector(
                                label = "Title",
                                options = when (currentPassenger.type) {
                                    "ADULT" -> listOf("Mr", "Mrs", "Ms", "Dr")
                                    "CHILD" -> listOf("Master", "Miss")
                                    else -> listOf("Infant")
                                },
                                selectedOption = currentPassenger.title,
                                onSelect = { viewModel.updatePassengerField(PassengerFormField.TITLE, it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.firstName,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.FIRST_NAME, it) },
                                    label = "First Name",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.lastName,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.LAST_NAME, it) },
                                    label = "Last Name",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Date of birth and nationality
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.dateOfBirth,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DATE_OF_BIRTH, it) },
                                    label = "Date of Birth",
                                    placeholder = "YYYY-MM-DD",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.nationality,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.NATIONALITY, it) },
                                    label = "Nationality",
                                    placeholder = "SA",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Travel Document Card
                    item {
                        VelocityFormCard(title = "Travel Document") {
                            // Document type chips
                            VelocityChipSelector(
                                label = "Document Type",
                                options = listOf("Passport", "National ID", "Iqama"),
                                selectedOption = when (currentPassenger.documentType) {
                                    "PASSPORT" -> "Passport"
                                    "NATIONAL_ID" -> "National ID"
                                    "IQAMA" -> "Iqama"
                                    else -> "Passport"
                                },
                                onSelect = { selected ->
                                    val code = when (selected) {
                                        "Passport" -> "PASSPORT"
                                        "National ID" -> "NATIONAL_ID"
                                        "Iqama" -> "IQAMA"
                                        else -> "PASSPORT"
                                    }
                                    viewModel.updatePassengerField(PassengerFormField.DOCUMENT_TYPE, code)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Document number and expiry
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.documentNumber,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DOCUMENT_NUMBER, it) },
                                    label = "Document Number",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.documentExpiry,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DOCUMENT_EXPIRY, it) },
                                    label = "Expiry Date",
                                    placeholder = "YYYY-MM-DD",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Contact Info Card (only for primary adult)
                    if (currentPassenger.id == "adult_0") {
                        item {
                            VelocityFormCard(title = "Contact Information") {
                                VelocityGlassTextField(
                                    value = currentPassenger.email,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.EMAIL, it) },
                                    label = "Email Address",
                                    placeholder = "your@email.com",
                                    keyboardType = KeyboardType.Email
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                VelocityGlassTextField(
                                    value = currentPassenger.phone,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.PHONE, it) },
                                    label = "Phone Number",
                                    placeholder = "+966 5XX XXX XXXX",
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                        }
                    }

                    // Error message
                    if (state.error != null) {
                        item {
                            Surface(
                                color = VelocityColors.Error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.error ?: "",
                                        style = VelocityTheme.typography.body,
                                        color = VelocityColors.Error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearPassengerError() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = VelocityColors.Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // Bottom navigation with glassmorphic buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    VelocityColors.BackgroundDeep.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!state.isFirstPassenger) {
                            // Previous button - glass style
                            Surface(
                                onClick = { viewModel.previousPassenger() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = VelocityColors.GlassBg,
                                border = BorderStroke(1.dp, VelocityColors.GlassBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Previous",
                                        style = VelocityTheme.typography.button,
                                        color = VelocityColors.TextMain
                                    )
                                }
                            }
                        }

                        // Continue/Next button - accent style
                        Surface(
                            onClick = {
                                if (state.isLastPassenger) {
                                    if (viewModel.validatePassengers()) {
                                        onContinue()
                                    }
                                } else {
                                    viewModel.nextPassenger()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = VelocityColors.Accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state.isLastPassenger) "Continue to Payment" else "Next Passenger",
                                    style = VelocityTheme.typography.button,
                                    color = VelocityColors.BackgroundDeep
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glassmorphic form card container.
 */
@Composable
private fun VelocityFormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = VelocityTheme.typography.body,
                color = VelocityColors.TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Glassmorphic text field matching Velocity design.
 */
@Composable
private fun VelocityGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Chip selector for options like Title, Document Type.
 */
@Composable
private fun VelocityChipSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) VelocityColors.Accent else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder
                    )
                ) {
                    Text(
                        text = option,
                        style = VelocityTheme.typography.button,
                        color = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WasmPaymentScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.paymentState.collectAsState()
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMain
                    )
                }
                Text(
                    text = "Payment",
                    style = VelocityTheme.typography.timeBig,
                    color = VelocityColors.TextMain
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Booking summary card
                item {
                    Surface(
                        color = VelocityColors.GlassBg,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Booking Summary",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Flight",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = state.flightNumber,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Fare",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = state.fareFamily,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Passengers",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = "${state.passengerCount} × ${state.currency} ${state.pricePerPerson}",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = VelocityColors.GlassBorder
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = "${state.currency} ${state.totalPrice}",
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.Accent
                                )
                            }
                        }
                    }
                }

                // Payment form card
                item {
                    Surface(
                        color = VelocityColors.GlassBg,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Card Details",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            WasmTextField(
                                value = state.cardholderName,
                                onValueChange = { viewModel.updatePaymentField(PaymentFormField.CARDHOLDER_NAME, it) },
                                label = "Cardholder Name *"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            WasmTextField(
                                value = state.cardNumber,
                                onValueChange = { viewModel.updatePaymentField(PaymentFormField.CARD_NUMBER, it) },
                                label = "Card Number *",
                                placeholder = "•••• •••• •••• ••••",
                                keyboardType = KeyboardType.Number
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                WasmTextField(
                                    value = state.expiryDate,
                                    onValueChange = { viewModel.updatePaymentField(PaymentFormField.EXPIRY, it) },
                                    label = "Expiry *",
                                    placeholder = "MM/YY",
                                    modifier = Modifier.weight(1f)
                                )
                                WasmTextField(
                                    value = state.cvv,
                                    onValueChange = { viewModel.updatePaymentField(PaymentFormField.CVV, it) },
                                    label = "CVV *",
                                    placeholder = "123",
                                    keyboardType = KeyboardType.Number,
                                    isPassword = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Error message
                if (state.error != null) {
                    item {
                        Surface(
                            color = VelocityColors.Error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.error ?: "",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.Error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearPaymentError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = VelocityColors.Error
                                    )
                                }
                            }
                        }
                    }
                }

                // Secure payment notice
                item {
                    Surface(
                        color = VelocityColors.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = VelocityColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your payment information is encrypted and secure. We never store your full card details.",
                                style = VelocityTheme.typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Pay button
            Surface(
                color = VelocityColors.GlassBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.processPayment(onSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !state.isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VelocityColors.BackgroundDeep,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (state.isProcessing) "Processing..." else "Pay ${state.currency} ${state.totalPrice}"
                    )
                }
            }
        }
    }
}

@Composable
private fun WasmConfirmationScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onNewBooking: () -> Unit
) {
    val state by viewModel.confirmationState.collectAsState()
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success header
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VelocityColors.Success.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = VelocityColors.Success,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Booking Confirmed!",
                    style = VelocityTheme.typography.heroTitle,
                    color = VelocityColors.Success
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your flight has been successfully booked",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // PNR Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.Accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Booking Reference (PNR)",
                            style = VelocityTheme.typography.duration,
                            color = VelocityColors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pnr,
                            style = VelocityTheme.typography.heroTitle,
                            color = VelocityColors.Accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Save this reference for managing your booking",
                            style = VelocityTheme.typography.labelSmall,
                            color = VelocityColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Flight details card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = VelocityColors.Accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Flight Details",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = state.departureTime,
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = state.originCode,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                                Text(
                                    text = state.originCity,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.flightNumber,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.Accent
                                )
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = VelocityColors.Accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Direct",
                                    style = VelocityTheme.typography.labelSmall,
                                    color = VelocityColors.TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = state.arrivalTime,
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = state.destinationCode,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                                Text(
                                    text = state.destinationCity,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = VelocityColors.GlassBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.departureDate,
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }
                    }
                }
            }

            // Passenger info card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VelocityColors.Accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Passengers",
                                style = VelocityTheme.typography.duration,
                                color = VelocityColors.TextMuted
                            )
                            Text(
                                text = "${state.primaryPassengerName}${if (state.passengerCount > 1) " + ${state.passengerCount - 1} more" else ""}",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }
                    }
                }
            }

            // Payment summary card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = VelocityColors.Accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Paid",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }
                            Text(
                                text = "${state.currency} ${state.totalPrice}",
                                style = VelocityTheme.typography.timeBig,
                                color = VelocityColors.Accent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = when (state.bookingStatus) {
                                "CONFIRMED" -> VelocityColors.Success.copy(alpha = 0.2f)
                                "PENDING" -> VelocityColors.Warning.copy(alpha = 0.2f)
                                else -> VelocityColors.Error.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = state.bookingStatus,
                                style = VelocityTheme.typography.labelSmall,
                                color = when (state.bookingStatus) {
                                    "CONFIRMED" -> VelocityColors.Success
                                    "PENDING" -> VelocityColors.Warning
                                    else -> VelocityColors.Error
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNewBooking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    Text("Book Another Flight")
                }
            }

            // Info note
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = VelocityColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "A confirmation email with your e-ticket has been sent to your registered email address. Please check in online 24 hours before departure.",
                            style = VelocityTheme.typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WasmSettingsScreen(
    localizationState: LocalizationState,
    onBack: () -> Unit
) {
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMain
                    )
                }
                Text(
                    text = strings.settings,
                    style = VelocityTheme.typography.timeBig,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Language selection
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = VelocityColors.GlassBg,
                contentColor = VelocityColors.TextMain
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = strings.language,
                        style = VelocityTheme.typography.body.copy(
                            color = VelocityColors.TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // English option
                    LanguageOption(
                        name = "English",
                        isSelected = localizationState.currentLanguage == AppLanguage.ENGLISH,
                        onClick = { localizationState.setLanguage(AppLanguage.ENGLISH) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Arabic option
                    LanguageOption(
                        name = "العربية",
                        isSelected = localizationState.currentLanguage == AppLanguage.ARABIC,
                        onClick = { localizationState.setLanguage(AppLanguage.ARABIC) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = VelocityTheme.typography.body
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = VelocityColors.Accent
            )
        }
    }
}

@Composable
private fun WasmSavedBookingsPlaceholder(onBack: () -> Unit) {
    VelocityThemeWithBackground(isRtl = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMain
                    )
                }
                Text(
                    text = "Saved Bookings",
                    style = VelocityTheme.typography.timeBig,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No saved bookings",
                        style = VelocityTheme.typography.body
                    )
                    Text(
                        text = "Complete a booking to see it here",
                        style = VelocityTheme.typography.duration,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }
    }
}

// Helper composables for form fields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder) }} else null,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VelocityColors.Accent,
            unfocusedBorderColor = VelocityColors.GlassBorder,
            focusedLabelColor = VelocityColors.Accent,
            unfocusedLabelColor = VelocityColors.TextMuted,
            cursorColor = VelocityColors.Accent,
            focusedTextColor = VelocityColors.TextMain,
            unfocusedTextColor = VelocityColors.TextMain
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasmDropdownField(
    value: String,
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VelocityColors.Accent,
                unfocusedBorderColor = VelocityColors.GlassBorder,
                focusedLabelColor = VelocityColors.Accent,
                unfocusedLabelColor = VelocityColors.TextMuted,
                focusedTextColor = VelocityColors.TextMain,
                unfocusedTextColor = VelocityColors.TextMain
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
