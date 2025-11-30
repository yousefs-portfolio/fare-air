package com.flyadeal.app.ui.screens.passengers

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.state.BookingFlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * ScreenModel for the Passenger Info screen.
 * Handles passenger data entry and validation.
 */
class PassengerInfoScreenModel(
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(PassengerInfoUiState())
    val uiState: StateFlow<PassengerInfoUiState> = _uiState.asStateFlow()

    init {
        initializePassengers()
    }

    /**
     * Initializes passenger forms based on search criteria.
     */
    private fun initializePassengers() {
        screenModelScope.launch {
            val criteria = bookingFlowState.searchCriteria
            if (criteria == null) {
                _uiState.update { it.copy(error = "Search criteria not available") }
                return@launch
            }

            val passengers = mutableListOf<PassengerFormData>()

            // Add adult passengers
            repeat(criteria.passengers.adults) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "adult_$index",
                        type = PassengerType.ADULT,
                        label = "Adult ${index + 1}"
                    )
                )
            }

            // Add child passengers
            repeat(criteria.passengers.children) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "child_$index",
                        type = PassengerType.CHILD,
                        label = "Child ${index + 1}"
                    )
                )
            }

            // Add infant passengers
            repeat(criteria.passengers.infants) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "infant_$index",
                        type = PassengerType.INFANT,
                        label = "Infant ${index + 1}"
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    passengers = passengers,
                    currentPassengerIndex = 0
                )
            }
        }
    }

    /**
     * Updates a passenger field value.
     */
    fun updatePassengerField(passengerId: String, field: PassengerField, value: String) {
        _uiState.update { state ->
            val updatedPassengers = state.passengers.map { passenger ->
                if (passenger.id == passengerId) {
                    when (field) {
                        PassengerField.TITLE -> passenger.copy(title = value)
                        PassengerField.FIRST_NAME -> passenger.copy(firstName = value.uppercase())
                        PassengerField.LAST_NAME -> passenger.copy(lastName = value.uppercase())
                        PassengerField.DATE_OF_BIRTH -> passenger.copy(dateOfBirth = formatDateInput(value))
                        PassengerField.NATIONALITY -> passenger.copy(nationality = value.uppercase())
                        PassengerField.DOCUMENT_TYPE -> passenger.copy(documentType = value)
                        PassengerField.DOCUMENT_NUMBER -> passenger.copy(documentNumber = value.uppercase())
                        PassengerField.DOCUMENT_EXPIRY -> passenger.copy(documentExpiry = formatDateInput(value))
                        PassengerField.EMAIL -> passenger.copy(email = value.lowercase())
                        PassengerField.PHONE -> passenger.copy(phone = value)
                    }
                } else {
                    passenger
                }
            }
            state.copy(passengers = updatedPassengers, error = null)
        }
    }

    /**
     * Formats date input with automatic dash insertion.
     * Input: raw digits like "19860429"
     * Output: formatted "1986-04-29"
     */
    private fun formatDateInput(input: String): String {
        // Remove any existing dashes and non-digits
        val digits = input.filter { it.isDigit() }

        // Limit to 8 digits (YYYYMMDD)
        val limited = digits.take(8)

        return buildString {
            limited.forEachIndexed { index, char ->
                append(char)
                // Add dash after year (position 3) and month (position 5)
                if (index == 3 && limited.length > 4) append('-')
                if (index == 5 && limited.length > 6) append('-')
            }
        }
    }

    /**
     * Moves to the next passenger form.
     */
    fun nextPassenger() {
        _uiState.update { state ->
            val currentIndex = state.currentPassengerIndex
            if (currentIndex < state.passengers.size - 1) {
                state.copy(currentPassengerIndex = currentIndex + 1)
            } else {
                state
            }
        }
    }

    /**
     * Moves to the previous passenger form.
     */
    fun previousPassenger() {
        _uiState.update { state ->
            val currentIndex = state.currentPassengerIndex
            if (currentIndex > 0) {
                state.copy(currentPassengerIndex = currentIndex - 1)
            } else {
                state
            }
        }
    }

    /**
     * Navigates to a specific passenger by index.
     */
    fun goToPassenger(index: Int) {
        _uiState.update { state ->
            if (index in state.passengers.indices) {
                state.copy(currentPassengerIndex = index)
            } else {
                state
            }
        }
    }

    /**
     * Validates all passengers and proceeds if valid.
     */
    fun validateAndProceed(onValid: () -> Unit) {
        val state = _uiState.value
        val validationErrors = mutableListOf<String>()

        state.passengers.forEachIndexed { index, passenger ->
            val errors = validatePassenger(passenger)
            if (errors.isNotEmpty()) {
                validationErrors.add("${passenger.label}: ${errors.joinToString(", ")}")
            }
        }

        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(error = validationErrors.first())
            }
            // Navigate to first passenger with error
            val firstErrorIndex = state.passengers.indexOfFirst { validatePassenger(it).isNotEmpty() }
            if (firstErrorIndex >= 0) {
                goToPassenger(firstErrorIndex)
            }
            return
        }

        // Store passenger info in booking flow state
        bookingFlowState.setPassengerInfo(
            state.passengers.map { form ->
                com.flyadeal.app.state.PassengerInfo(
                    id = form.id,
                    type = form.type.name,
                    title = form.title,
                    firstName = form.firstName,
                    lastName = form.lastName,
                    dateOfBirth = form.dateOfBirth,
                    nationality = form.nationality,
                    documentType = form.documentType,
                    documentNumber = form.documentNumber,
                    documentExpiry = form.documentExpiry,
                    email = form.email,
                    phone = form.phone
                )
            }
        )

        onValid()
    }

    /**
     * Validates a single passenger form.
     */
    private fun validatePassenger(passenger: PassengerFormData): List<String> {
        val errors = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        if (passenger.title.isBlank()) {
            errors.add("Title is required")
        }
        if (passenger.firstName.isBlank()) {
            errors.add("First name is required")
        }
        if (passenger.lastName.isBlank()) {
            errors.add("Last name is required")
        }
        if (passenger.dateOfBirth.isBlank()) {
            errors.add("Date of birth is required")
        } else {
            val dobValidation = validateDateOfBirth(passenger.dateOfBirth, passenger.type, today)
            if (dobValidation != null) {
                errors.add(dobValidation)
            }
        }

        // Validate document number format
        if (passenger.documentNumber.isNotBlank()) {
            val docValidation = validateDocumentNumber(passenger.documentNumber, passenger.documentType)
            if (docValidation != null) {
                errors.add(docValidation)
            }

            // Validate expiry date if document number is provided
            if (passenger.documentExpiry.isBlank()) {
                errors.add("Document expiry date is required")
            } else {
                val expiryValidation = validateExpiryDate(passenger.documentExpiry, today)
                if (expiryValidation != null) {
                    errors.add(expiryValidation)
                }
            }
        }

        // Only validate contact info for first adult passenger
        if (passenger.type == PassengerType.ADULT && passenger.id == "adult_0") {
            if (passenger.email.isBlank()) {
                errors.add("Email is required")
            } else if (!isValidEmail(passenger.email)) {
                errors.add("Invalid email format")
            }
            if (passenger.phone.isBlank()) {
                errors.add("Phone number is required")
            }
        }

        return errors
    }

    /**
     * Validates date of birth based on passenger type.
     * Returns error message or null if valid.
     */
    private fun validateDateOfBirth(dateStr: String, type: PassengerType, today: LocalDate): String? {
        val date = parseDate(dateStr) ?: return "Invalid date format (use YYYY-MM-DD)"

        // Must be in the past
        if (date >= today) {
            return "Date of birth must be in the past"
        }

        // Calculate age
        val age = calculateAge(date, today)

        // Validate age based on passenger type
        return when (type) {
            PassengerType.ADULT -> {
                if (age < 12) "Adult must be 12 years or older"
                else if (age > 120) "Invalid date of birth"
                else null
            }
            PassengerType.CHILD -> {
                if (age < 2) "Child must be at least 2 years old"
                else if (age >= 12) "Child must be under 12 years old"
                else null
            }
            PassengerType.INFANT -> {
                if (age >= 2) "Infant must be under 2 years old"
                else null
            }
        }
    }

    /**
     * Validates document expiry date.
     * Must be at least 6 months in the future for most destinations.
     */
    private fun validateExpiryDate(dateStr: String, today: LocalDate): String? {
        val date = parseDate(dateStr) ?: return "Invalid date format (use YYYY-MM-DD)"

        // Must be in the future
        if (date <= today) {
            return "Document has expired"
        }

        // Should be at least 6 months in the future (common travel requirement)
        val sixMonthsFromNow = LocalDate(
            year = if (today.monthNumber > 6) today.year + 1 else today.year,
            monthNumber = ((today.monthNumber + 5) % 12) + 1,
            dayOfMonth = minOf(today.dayOfMonth, 28) // Safe day to avoid month boundary issues
        )

        if (date < sixMonthsFromNow) {
            return "Document should be valid for at least 6 months"
        }

        return null
    }

    /**
     * Validates document number based on document type.
     * Returns error message or null if valid.
     */
    private fun validateDocumentNumber(docNumber: String, docType: String): String? {
        val cleanNumber = docNumber.trim().uppercase()

        return when (docType) {
            "PASSPORT" -> validatePassportNumber(cleanNumber)
            "NATIONAL_ID" -> validateSaudiNationalId(cleanNumber)
            "IQAMA" -> validateIqamaNumber(cleanNumber)
            else -> null
        }
    }

    /**
     * Validates passport number format.
     * Most passports: 6-9 alphanumeric characters.
     */
    private fun validatePassportNumber(number: String): String? {
        if (number.length < 6) {
            return "Passport number too short (min 6 characters)"
        }
        if (number.length > 12) {
            return "Passport number too long (max 12 characters)"
        }
        if (!number.all { it.isLetterOrDigit() }) {
            return "Passport number should only contain letters and numbers"
        }
        return null
    }

    /**
     * Validates Saudi National ID (Huwiyya) format.
     * Saudi National ID: 10 digits starting with 1.
     */
    private fun validateSaudiNationalId(number: String): String? {
        if (number.length != 10) {
            return "Saudi National ID must be 10 digits"
        }
        if (!number.all { it.isDigit() }) {
            return "Saudi National ID must contain only numbers"
        }
        if (!number.startsWith("1")) {
            return "Saudi National ID must start with 1"
        }
        // Luhn algorithm check for Saudi ID
        if (!isValidSaudiIdChecksum(number)) {
            return "Invalid Saudi National ID number"
        }
        return null
    }

    /**
     * Validates Iqama (Resident ID) format.
     * Iqama: 10 digits starting with 2.
     */
    private fun validateIqamaNumber(number: String): String? {
        if (number.length != 10) {
            return "Iqama must be 10 digits"
        }
        if (!number.all { it.isDigit() }) {
            return "Iqama must contain only numbers"
        }
        if (!number.startsWith("2")) {
            return "Iqama must start with 2"
        }
        // Luhn algorithm check for Iqama
        if (!isValidSaudiIdChecksum(number)) {
            return "Invalid Iqama number"
        }
        return null
    }

    /**
     * Validates Saudi ID/Iqama checksum using Luhn algorithm variant.
     */
    private fun isValidSaudiIdChecksum(number: String): Boolean {
        if (number.length != 10) return false

        var sum = 0
        for (i in number.indices) {
            var digit = number[i].digitToInt()
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10)
                }
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    /**
     * Parses a date string in YYYY-MM-DD format.
     */
    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return null

            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null

            // Basic range validation
            if (year < 1900 || year > 2100) return null
            if (month < 1 || month > 12) return null
            if (day < 1 || day > 31) return null

            LocalDate(year, month, day)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates age in years between two dates.
     */
    private fun calculateAge(birthDate: LocalDate, today: LocalDate): Int {
        var age = today.year - birthDate.year
        if (today.monthNumber < birthDate.monthNumber ||
            (today.monthNumber == birthDate.monthNumber && today.dayOfMonth < birthDate.dayOfMonth)) {
            age--
        }
        return age
    }

    /**
     * Basic email validation.
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Passenger Info screen.
 */
data class PassengerInfoUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val passengers: List<PassengerFormData> = emptyList(),
    val currentPassengerIndex: Int = 0
) {
    val currentPassenger: PassengerFormData?
        get() = passengers.getOrNull(currentPassengerIndex)

    val isFirstPassenger: Boolean
        get() = currentPassengerIndex == 0

    val isLastPassenger: Boolean
        get() = currentPassengerIndex == passengers.size - 1

    val progress: Float
        get() = if (passengers.isEmpty()) 0f else (currentPassengerIndex + 1).toFloat() / passengers.size
}

/**
 * Form data for a single passenger.
 */
data class PassengerFormData(
    val id: String,
    val type: PassengerType,
    val label: String,
    val title: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val nationality: String = "SA",
    val documentType: String = "PASSPORT",
    val documentNumber: String = "",
    val documentExpiry: String = "",
    val email: String = "",
    val phone: String = ""
)

/**
 * Passenger types with age ranges.
 */
enum class PassengerType(val ageRange: String) {
    ADULT("12+ years"),
    CHILD("2-11 years"),
    INFANT("Under 2 years")
}

/**
 * Fields in the passenger form.
 */
enum class PassengerField {
    TITLE,
    FIRST_NAME,
    LAST_NAME,
    DATE_OF_BIRTH,
    NATIONALITY,
    DOCUMENT_TYPE,
    DOCUMENT_NUMBER,
    DOCUMENT_EXPIRY,
    EMAIL,
    PHONE
}
