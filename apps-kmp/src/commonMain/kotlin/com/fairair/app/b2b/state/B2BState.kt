package com.fairair.app.b2b.state

import com.fairair.app.b2b.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Centralized state management for B2B agency portal.
 * Manages authentication, flight search, bookings, and agency operations.
 */
class B2BState(
    private val apiClient: B2BApiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ========================================
    // AUTHENTICATION STATE
    // ========================================

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<AgencyUserDto?>(null)
    val currentUser: StateFlow<AgencyUserDto?> = _currentUser.asStateFlow()

    private val _currentAgency = MutableStateFlow<AgencyDto?>(null)
    val currentAgency: StateFlow<AgencyDto?> = _currentAgency.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    // ========================================
    // ROUTES & STATIONS STATE
    // ========================================

    private val _routes = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val routes: StateFlow<Map<String, List<String>>> = _routes.asStateFlow()

    private val _stations = MutableStateFlow<List<StationDto>>(emptyList())
    val stations: StateFlow<List<StationDto>> = _stations.asStateFlow()

    private val _isLoadingRoutes = MutableStateFlow(false)
    val isLoadingRoutes: StateFlow<Boolean> = _isLoadingRoutes.asStateFlow()

    // ========================================
    // FLIGHT SEARCH STATE
    // ========================================

    private val _searchResults = MutableStateFlow<FlightSearchResponse?>(null)
    val searchResults: StateFlow<FlightSearchResponse?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _lowFareCalendar = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lowFareCalendar: StateFlow<Map<String, Long>> = _lowFareCalendar.asStateFlow()

    // ========================================
    // BOOKING STATE
    // ========================================

    private val _selectedOutboundFlight = MutableStateFlow<FlightOptionDto?>(null)
    val selectedOutboundFlight: StateFlow<FlightOptionDto?> = _selectedOutboundFlight.asStateFlow()

    private val _selectedOutboundFare = MutableStateFlow<FareDto?>(null)
    val selectedOutboundFare: StateFlow<FareDto?> = _selectedOutboundFare.asStateFlow()

    private val _selectedReturnFlight = MutableStateFlow<FlightOptionDto?>(null)
    val selectedReturnFlight: StateFlow<FlightOptionDto?> = _selectedReturnFlight.asStateFlow()

    private val _selectedReturnFare = MutableStateFlow<FareDto?>(null)
    val selectedReturnFare: StateFlow<FareDto?> = _selectedReturnFare.asStateFlow()

    private val _passengers = MutableStateFlow<List<PassengerDto>>(emptyList())
    val passengers: StateFlow<List<PassengerDto>> = _passengers.asStateFlow()

    private val _isBooking = MutableStateFlow(false)
    val isBooking: StateFlow<Boolean> = _isBooking.asStateFlow()

    private val _bookingConfirmation = MutableStateFlow<BookingConfirmation?>(null)
    val bookingConfirmation: StateFlow<BookingConfirmation?> = _bookingConfirmation.asStateFlow()

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError.asStateFlow()

    // ========================================
    // BOOKINGS LIST STATE
    // ========================================

    private val _agencyBookings = MutableStateFlow<List<BookingSummaryDto>>(emptyList())
    val agencyBookings: StateFlow<List<BookingSummaryDto>> = _agencyBookings.asStateFlow()

    private val _isLoadingBookings = MutableStateFlow(false)
    val isLoadingBookings: StateFlow<Boolean> = _isLoadingBookings.asStateFlow()

    private val _selectedBooking = MutableStateFlow<BookingDetails?>(null)
    val selectedBooking: StateFlow<BookingDetails?> = _selectedBooking.asStateFlow()

    private val _bookingsPage = MutableStateFlow(0)
    val bookingsPage: StateFlow<Int> = _bookingsPage.asStateFlow()

    private val _totalBookingsPages = MutableStateFlow(0)
    val totalBookingsPages: StateFlow<Int> = _totalBookingsPages.asStateFlow()

    // ========================================
    // GROUP BOOKING REQUESTS STATE
    // ========================================

    private val _groupBookingRequests = MutableStateFlow<List<GroupBookingRequestDto>>(emptyList())
    val groupBookingRequests: StateFlow<List<GroupBookingRequestDto>> = _groupBookingRequests.asStateFlow()

    private val _isLoadingGroupRequests = MutableStateFlow(false)
    val isLoadingGroupRequests: StateFlow<Boolean> = _isLoadingGroupRequests.asStateFlow()

    private val _groupRequestError = MutableStateFlow<String?>(null)
    val groupRequestError: StateFlow<String?> = _groupRequestError.asStateFlow()

    // ========================================
    // CHARTER REQUESTS STATE
    // ========================================

    private val _charterRequests = MutableStateFlow<List<CharterRequestDto>>(emptyList())
    val charterRequests: StateFlow<List<CharterRequestDto>> = _charterRequests.asStateFlow()

    private val _isLoadingCharterRequests = MutableStateFlow(false)
    val isLoadingCharterRequests: StateFlow<Boolean> = _isLoadingCharterRequests.asStateFlow()

    private val _charterRequestError = MutableStateFlow<String?>(null)
    val charterRequestError: StateFlow<String?> = _charterRequestError.asStateFlow()

    // ========================================
    // AGENCY PROFILE STATE
    // ========================================

    private val _agencyProfile = MutableStateFlow<AgencyProfileDto?>(null)
    val agencyProfile: StateFlow<AgencyProfileDto?> = _agencyProfile.asStateFlow()

    private val _agencyUsers = MutableStateFlow<List<AgencyUserDto>>(emptyList())
    val agencyUsers: StateFlow<List<AgencyUserDto>> = _agencyUsers.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    // ========================================
    // STATS & REPORTS STATE
    // ========================================

    private val _agencyStats = MutableStateFlow<AgencyStatsResponse?>(null)
    val agencyStats: StateFlow<AgencyStatsResponse?> = _agencyStats.asStateFlow()

    private val _commissionReport = MutableStateFlow<CommissionReportResponse?>(null)
    val commissionReport: StateFlow<CommissionReportResponse?> = _commissionReport.asStateFlow()

    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats.asStateFlow()

    // ========================================
    // GENERAL ERROR STATE
    // ========================================

    private val _generalError = MutableStateFlow<String?>(null)
    val generalError: StateFlow<String?> = _generalError.asStateFlow()

    // ========================================
    // AUTHENTICATION ACTIONS
    // ========================================

    /**
     * Authenticate with email and password.
     */
    fun login(email: String, password: String) {
        scope.launch {
            _isLoggingIn.value = true
            _authError.value = null

            apiClient.login(email, password)
                .onSuccess { response ->
                    _currentUser.value = response.user
                    _currentAgency.value = response.agency
                    _isLoggedIn.value = true
                    // Load initial data after login
                    loadRoutes()
                }
                .onFailure { error ->
                    _authError.value = error.message ?: "Login failed"
                }

            _isLoggingIn.value = false
        }
    }

    /**
     * Register a new agency.
     */
    fun registerAgency(request: AgencyRegistrationRequest, onSuccess: () -> Unit) {
        scope.launch {
            _isLoggingIn.value = true
            _authError.value = null

            apiClient.registerAgency(request)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _authError.value = error.message ?: "Registration failed"
                }

            _isLoggingIn.value = false
        }
    }

    /**
     * Log out and clear all state.
     */
    fun logout() {
        apiClient.clearAuth()
        _isLoggedIn.value = false
        _currentUser.value = null
        _currentAgency.value = null
        _searchResults.value = null
        _agencyBookings.value = emptyList()
        _groupBookingRequests.value = emptyList()
        _charterRequests.value = emptyList()
        _agencyProfile.value = null
        _agencyStats.value = null
        clearErrors()
    }

    // ========================================
    // ROUTES & FLIGHT SEARCH ACTIONS
    // ========================================

    /**
     * Load available routes from API.
     */
    fun loadRoutes() {
        scope.launch {
            _isLoadingRoutes.value = true

            apiClient.getRoutes()
                .onSuccess { response ->
                    _routes.value = response.routes
                    _stations.value = response.stations
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load routes"
                }

            _isLoadingRoutes.value = false
        }
    }

    /**
     * Get destinations for a specific origin.
     */
    fun getDestinationsForOrigin(origin: String): List<StationDto> {
        val destinationCodes = _routes.value[origin] ?: emptyList()
        return _stations.value.filter { it.code in destinationCodes }
    }

    /**
     * Get station by code.
     */
    fun getStation(code: String): StationDto? {
        return _stations.value.find { it.code == code }
    }

    /**
     * Search for flights.
     */
    fun searchFlights(request: FlightSearchRequest) {
        scope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResults.value = null

            apiClient.searchFlights(request)
                .onSuccess { response ->
                    _searchResults.value = response
                }
                .onFailure { error ->
                    _searchError.value = error.message ?: "Search failed"
                }

            _isSearching.value = false
        }
    }

    /**
     * Load low fare calendar for a route.
     */
    fun loadLowFareCalendar(origin: String, destination: String, startDate: String, endDate: String) {
        scope.launch {
            apiClient.getLowFareCalendar(origin, destination, startDate, endDate)
                .onSuccess { response ->
                    _lowFareCalendar.value = response.fares
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load fares"
                }
        }
    }

    // ========================================
    // BOOKING SELECTION ACTIONS
    // ========================================

    /**
     * Select an outbound flight and fare.
     */
    fun selectOutboundFlight(flight: FlightOptionDto, fare: FareDto) {
        _selectedOutboundFlight.value = flight
        _selectedOutboundFare.value = fare
    }

    /**
     * Select a return flight and fare.
     */
    fun selectReturnFlight(flight: FlightOptionDto, fare: FareDto) {
        _selectedReturnFlight.value = flight
        _selectedReturnFare.value = fare
    }

    /**
     * Clear flight selection.
     */
    fun clearFlightSelection() {
        _selectedOutboundFlight.value = null
        _selectedOutboundFare.value = null
        _selectedReturnFlight.value = null
        _selectedReturnFare.value = null
        _passengers.value = emptyList()
    }

    /**
     * Update passengers list.
     */
    fun updatePassengers(passengers: List<PassengerDto>) {
        _passengers.value = passengers
    }

    /**
     * Create a booking.
     */
    fun createBooking(
        contactEmail: String,
        contactPhone: String,
        clientReference: String?
    ) {
        val searchId = _searchResults.value?.searchId ?: return
        val outboundFlight = _selectedOutboundFlight.value ?: return
        val outboundFare = _selectedOutboundFare.value ?: return
        val passengerList = _passengers.value

        if (passengerList.isEmpty()) {
            _bookingError.value = "Please add passengers"
            return
        }

        scope.launch {
            _isBooking.value = true
            _bookingError.value = null
            _bookingConfirmation.value = null

            val request = B2BBookingRequest(
                searchId = searchId,
                outboundFlightId = outboundFlight.flightId,
                outboundFareId = outboundFare.fareId,
                returnFlightId = _selectedReturnFlight.value?.flightId,
                returnFareId = _selectedReturnFare.value?.fareId,
                passengers = passengerList,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                paymentMethod = "CREDIT_ACCOUNT",
                clientReference = clientReference
            )

            apiClient.createBooking(request)
                .onSuccess { confirmation ->
                    _bookingConfirmation.value = confirmation
                    // Refresh bookings list
                    loadAgencyBookings()
                    // Refresh agency to update available credit
                    loadAgencyProfile()
                }
                .onFailure { error ->
                    _bookingError.value = error.message ?: "Booking failed"
                }

            _isBooking.value = false
        }
    }

    // ========================================
    // BOOKINGS LIST ACTIONS
    // ========================================

    /**
     * Load agency bookings.
     */
    fun loadAgencyBookings(page: Int = 0, status: String? = null) {
        scope.launch {
            _isLoadingBookings.value = true

            apiClient.getAgencyBookings(page, 20, status)
                .onSuccess { response ->
                    _agencyBookings.value = response.bookings
                    _bookingsPage.value = response.page
                    _totalBookingsPages.value = response.totalPages
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load bookings"
                }

            _isLoadingBookings.value = false
        }
    }

    /**
     * Load a specific booking by PNR.
     */
    fun loadBookingDetails(pnr: String) {
        scope.launch {
            _isLoadingBookings.value = true

            apiClient.getBooking(pnr)
                .onSuccess { booking ->
                    _selectedBooking.value = booking
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load booking"
                }

            _isLoadingBookings.value = false
        }
    }

    /**
     * Clear selected booking.
     */
    fun clearSelectedBooking() {
        _selectedBooking.value = null
    }

    /**
     * Cancel a booking.
     */
    fun cancelBooking(pnr: String, reason: String, onSuccess: () -> Unit) {
        scope.launch {
            _isLoadingBookings.value = true

            apiClient.cancelBooking(pnr, reason)
                .onSuccess {
                    loadAgencyBookings()
                    onSuccess()
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Cancellation failed"
                }

            _isLoadingBookings.value = false
        }
    }

    // ========================================
    // GROUP BOOKING REQUEST ACTIONS
    // ========================================

    /**
     * Load group booking requests.
     */
    fun loadGroupBookingRequests() {
        scope.launch {
            _isLoadingGroupRequests.value = true

            apiClient.getGroupBookingRequests()
                .onSuccess { requests ->
                    _groupBookingRequests.value = requests
                }
                .onFailure { error ->
                    _groupRequestError.value = error.message ?: "Failed to load requests"
                }

            _isLoadingGroupRequests.value = false
        }
    }

    /**
     * Submit a group booking request.
     */
    fun submitGroupBookingRequest(request: GroupBookingRequest, onSuccess: () -> Unit) {
        scope.launch {
            _isLoadingGroupRequests.value = true
            _groupRequestError.value = null

            apiClient.submitGroupBookingRequest(request)
                .onSuccess {
                    loadGroupBookingRequests()
                    onSuccess()
                }
                .onFailure { error ->
                    _groupRequestError.value = error.message ?: "Submission failed"
                }

            _isLoadingGroupRequests.value = false
        }
    }

    /**
     * Accept a group booking quote.
     */
    fun acceptGroupQuote(requestId: String, onSuccess: () -> Unit) {
        scope.launch {
            apiClient.acceptGroupQuote(requestId)
                .onSuccess {
                    loadGroupBookingRequests()
                    onSuccess()
                }
                .onFailure { error ->
                    _groupRequestError.value = error.message ?: "Failed to accept quote"
                }
        }
    }

    /**
     * Reject a group booking quote.
     */
    fun rejectGroupQuote(requestId: String, onSuccess: () -> Unit) {
        scope.launch {
            apiClient.rejectGroupQuote(requestId)
                .onSuccess {
                    loadGroupBookingRequests()
                    onSuccess()
                }
                .onFailure { error ->
                    _groupRequestError.value = error.message ?: "Failed to reject quote"
                }
        }
    }

    // ========================================
    // CHARTER REQUEST ACTIONS
    // ========================================

    /**
     * Load charter requests.
     */
    fun loadCharterRequests() {
        scope.launch {
            _isLoadingCharterRequests.value = true

            apiClient.getCharterRequests()
                .onSuccess { requests ->
                    _charterRequests.value = requests
                }
                .onFailure { error ->
                    _charterRequestError.value = error.message ?: "Failed to load requests"
                }

            _isLoadingCharterRequests.value = false
        }
    }

    /**
     * Submit a charter request.
     */
    fun submitCharterRequest(request: CharterRequest, onSuccess: () -> Unit) {
        scope.launch {
            _isLoadingCharterRequests.value = true
            _charterRequestError.value = null

            apiClient.submitCharterRequest(request)
                .onSuccess {
                    loadCharterRequests()
                    onSuccess()
                }
                .onFailure { error ->
                    _charterRequestError.value = error.message ?: "Submission failed"
                }

            _isLoadingCharterRequests.value = false
        }
    }

    // ========================================
    // AGENCY PROFILE ACTIONS
    // ========================================

    /**
     * Load agency profile.
     */
    fun loadAgencyProfile() {
        scope.launch {
            _isLoadingProfile.value = true

            apiClient.getAgencyProfile()
                .onSuccess { profile ->
                    _agencyProfile.value = profile
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load profile"
                }

            _isLoadingProfile.value = false
        }
    }

    /**
     * Update agency profile.
     */
    fun updateAgencyProfile(request: UpdateAgencyProfileRequest, onSuccess: () -> Unit) {
        scope.launch {
            _isLoadingProfile.value = true

            apiClient.updateAgencyProfile(request)
                .onSuccess { profile ->
                    _agencyProfile.value = profile
                    onSuccess()
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to update profile"
                }

            _isLoadingProfile.value = false
        }
    }

    /**
     * Load agency users.
     */
    fun loadAgencyUsers() {
        scope.launch {
            apiClient.getAgencyUsers()
                .onSuccess { users ->
                    _agencyUsers.value = users
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load users"
                }
        }
    }

    /**
     * Invite a new user.
     */
    fun inviteUser(request: InviteUserRequest, onSuccess: () -> Unit) {
        scope.launch {
            apiClient.inviteUser(request)
                .onSuccess {
                    loadAgencyUsers()
                    onSuccess()
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to invite user"
                }
        }
    }

    // ========================================
    // STATS & REPORTS ACTIONS
    // ========================================

    /**
     * Load agency statistics with default period (current month).
     */
    fun loadAgencyStats() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val startDate = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-01"
        val endDate = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        loadAgencyStats(startDate, endDate)
    }

    /**
     * Load agency statistics for a specific period.
     */
    fun loadAgencyStats(startDate: String, endDate: String) {
        scope.launch {
            _isLoadingStats.value = true

            apiClient.getBookingStats(startDate, endDate)
                .onSuccess { stats ->
                    _agencyStats.value = stats
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load stats"
                }

            _isLoadingStats.value = false
        }
    }

    /**
     * Load commission report for current month.
     */
    fun loadCommissionReport() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        loadCommissionReport(now.monthNumber, now.year)
    }

    /**
     * Load commission report for a specific month.
     */
    fun loadCommissionReport(month: Int, year: Int) {
        scope.launch {
            _isLoadingStats.value = true

            apiClient.getCommissionReport(month, year)
                .onSuccess { report ->
                    _commissionReport.value = report
                }
                .onFailure { error ->
                    _generalError.value = error.message ?: "Failed to load commission report"
                }

            _isLoadingStats.value = false
        }
    }

    // ========================================
    // ERROR HANDLING
    // ========================================

    /**
     * Clear all errors.
     */
    fun clearErrors() {
        _authError.value = null
        _searchError.value = null
        _bookingError.value = null
        _groupRequestError.value = null
        _charterRequestError.value = null
        _generalError.value = null
    }

    /**
     * Clear a specific error.
     */
    fun clearAuthError() {
        _authError.value = null
    }

    fun clearSearchError() {
        _searchError.value = null
    }

    fun clearBookingError() {
        _bookingError.value = null
    }

    fun clearGeneralError() {
        _generalError.value = null
    }
}
