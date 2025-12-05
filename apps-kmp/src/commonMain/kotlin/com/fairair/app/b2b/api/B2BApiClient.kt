package com.fairair.app.b2b.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP client for B2B agency portal API calls.
 * Handles authentication, flight search, booking, and agency-specific operations.
 */
class B2BApiClient(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var authToken: String? = null
    private var agencyId: String? = null

    /**
     * Sets the authentication token for API requests.
     */
    fun setAuthToken(token: String, agency: String) {
        authToken = token
        agencyId = agency
    }

    /**
     * Clears the authentication token on logout.
     */
    fun clearAuth() {
        authToken = null
        agencyId = null
    }

    /**
     * Checks if the user is authenticated.
     */
    fun isAuthenticated(): Boolean = authToken != null

    // ========================================
    // AUTHENTICATION
    // ========================================

    /**
     * Authenticate agency user with email and password.
     */
    suspend fun login(email: String, password: String): Result<AgencyLoginResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(AgencyLoginRequest(email, password))
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val loginResponse = response.body<AgencyLoginResponse>()
                    setAuthToken(loginResponse.token, loginResponse.agency.id)
                    Result.success(loginResponse)
                }
                HttpStatusCode.Unauthorized -> Result.failure(Exception("Invalid credentials"))
                else -> Result.failure(Exception("Login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a new travel agency.
     */
    suspend fun registerAgency(request: AgencyRegistrationRequest): Result<AgencyRegistrationResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/auth/register-agency") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    Result.success(response.body())
                }
                HttpStatusCode.Conflict -> Result.failure(Exception("Agency with this email already exists"))
                HttpStatusCode.BadRequest -> Result.failure(Exception("Invalid registration data"))
                else -> Result.failure(Exception("Registration failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // FLIGHT SEARCH
    // ========================================

    /**
     * Get all available routes.
     */
    suspend fun getRoutes(): Result<RoutesResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/config/routes") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get routes: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get destinations from a specific origin.
     */
    suspend fun getDestinations(origin: String): Result<List<StationDto>> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/config/destinations/$origin") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get destinations: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for available flights.
     */
    suspend fun searchFlights(request: FlightSearchRequest): Result<FlightSearchResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/search") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Flight search failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get low fare calendar for a route.
     */
    suspend fun getLowFareCalendar(
        origin: String,
        destination: String,
        startDate: String,
        endDate: String
    ): Result<LowFareCalendarResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/search/low-fare-calendar") {
                header("Authorization", "Bearer $authToken")
                parameter("origin", origin)
                parameter("destination", destination)
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get low fare calendar: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // BOOKING
    // ========================================

    /**
     * Create a new booking on behalf of a client.
     */
    suspend fun createBooking(request: B2BBookingRequest): Result<BookingConfirmation> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/bookings") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.BadRequest -> Result.failure(Exception("Invalid booking data"))
                HttpStatusCode.Conflict -> Result.failure(Exception("Flights no longer available"))
                else -> Result.failure(Exception("Booking failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all bookings made by this agency.
     */
    suspend fun getAgencyBookings(
        page: Int = 0,
        size: Int = 20,
        status: String? = null
    ): Result<PagedBookingsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/bookings") {
                header("Authorization", "Bearer $authToken")
                parameter("page", page)
                parameter("size", size)
                status?.let { parameter("status", it) }
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get bookings: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific booking by PNR.
     */
    suspend fun getBooking(pnr: String): Result<BookingDetails> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/bookings/$pnr") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else if (response.status == HttpStatusCode.NotFound) {
                Result.failure(Exception("Booking not found"))
            } else {
                Result.failure(Exception("Failed to get booking: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel a booking.
     */
    suspend fun cancelBooking(pnr: String, reason: String): Result<CancellationResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/manage/$pnr/cancel") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(CancelBookingRequest(reason))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Cancellation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // ANCILLARY SERVICES
    // ========================================

    /**
     * Get seat map for a flight.
     */
    suspend fun getSeatMap(flightNumber: String): Result<SeatMapResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/seats/$flightNumber") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get seat map: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reserve seats for passengers.
     */
    suspend fun reserveSeats(request: SeatReservationRequest): Result<SeatReservationResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/seats/reserve") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Seat reservation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available meals for a flight.
     */
    suspend fun getMeals(): Result<List<MealDto>> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/meals") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get meals: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add ancillary services to a booking.
     */
    suspend fun addAncillaries(
        pnr: String,
        request: AddAncillariesRequest
    ): Result<BookingDetails> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/manage/$pnr/ancillaries") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to add ancillaries: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // GROUP BOOKING REQUESTS
    // ========================================

    /**
     * Submit a group booking request.
     */
    suspend fun submitGroupBookingRequest(request: GroupBookingRequest): Result<GroupBookingRequestResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/group-bookings") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.BadRequest -> Result.failure(Exception("Invalid request data"))
                else -> Result.failure(Exception("Request submission failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all group booking requests for this agency.
     */
    suspend fun getGroupBookingRequests(): Result<List<GroupBookingRequestDto>> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/group-bookings/agency") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get requests: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept a quote for a group booking request.
     */
    suspend fun acceptGroupQuote(requestId: String): Result<GroupBookingRequestDto> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/group-bookings/$requestId/accept-quote") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to accept quote: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject a quote for a group booking request.
     */
    suspend fun rejectGroupQuote(requestId: String): Result<GroupBookingRequestDto> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/group-bookings/$requestId/reject-quote") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to reject quote: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // CHARTER REQUESTS
    // ========================================

    /**
     * Submit a charter flight request.
     */
    suspend fun submitCharterRequest(request: CharterRequest): Result<CharterRequestResponse> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/charter-requests") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.BadRequest -> Result.failure(Exception("Invalid request data"))
                else -> Result.failure(Exception("Request submission failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all charter requests for this agency.
     */
    suspend fun getCharterRequests(): Result<List<CharterRequestDto>> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/charter-requests/agency") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get charter requests: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // AGENCY PROFILE
    // ========================================

    /**
     * Get current agency profile.
     */
    suspend fun getAgencyProfile(): Result<AgencyProfileDto> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/agencies/profile") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get profile: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update agency profile.
     */
    suspend fun updateAgencyProfile(request: UpdateAgencyProfileRequest): Result<AgencyProfileDto> {
        return try {
            val response = httpClient.put("$baseUrl/api/v1/b2b/agencies/profile") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to update profile: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get agency users/staff.
     */
    suspend fun getAgencyUsers(): Result<List<AgencyUserDto>> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/agencies/users") {
                header("Authorization", "Bearer $authToken")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get users: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invite a new user to the agency.
     */
    suspend fun inviteUser(request: InviteUserRequest): Result<AgencyUserDto> {
        return try {
            val response = httpClient.post("$baseUrl/api/v1/b2b/agencies/users/invite") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.Conflict -> Result.failure(Exception("User already exists"))
                else -> Result.failure(Exception("Failed to invite user: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // REPORTS & ANALYTICS
    // ========================================

    /**
     * Get agency booking statistics.
     */
    suspend fun getBookingStats(
        startDate: String,
        endDate: String
    ): Result<AgencyStatsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/reports/stats") {
                header("Authorization", "Bearer $authToken")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get stats: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get agency commission report.
     */
    suspend fun getCommissionReport(
        month: Int,
        year: Int
    ): Result<CommissionReportResponse> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/reports/commission") {
                header("Authorization", "Bearer $authToken")
                parameter("month", month)
                parameter("year", year)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get commission report: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download booking report as CSV.
     */
    suspend fun downloadBookingReport(
        startDate: String,
        endDate: String,
        format: String = "csv"
    ): Result<ByteArray> {
        return try {
            val response = httpClient.get("$baseUrl/api/v1/b2b/reports/bookings/download") {
                header("Authorization", "Bearer $authToken")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
                parameter("format", format)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to download report: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
