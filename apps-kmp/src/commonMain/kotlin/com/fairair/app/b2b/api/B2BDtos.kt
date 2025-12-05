package com.fairair.app.b2b.api

import kotlinx.serialization.Serializable

// ========================================
// AUTHENTICATION DTOs
// ========================================

@Serializable
data class AgencyLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AgencyLoginResponse(
    val token: String,
    val expiresIn: Long,
    val agency: AgencyDto,
    val user: AgencyUserDto
)

@Serializable
data class AgencyRegistrationRequest(
    val agencyName: String,
    val iataCode: String,
    val businessLicenseNumber: String,
    val contactPersonName: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String,
    val country: String,
    val password: String
)

@Serializable
data class AgencyRegistrationResponse(
    val message: String,
    val agencyId: String,
    val status: String // "PENDING_APPROVAL"
)

// ========================================
// AGENCY DTOs
// ========================================

@Serializable
data class AgencyDto(
    val id: String,
    val name: String,
    val iataCode: String,
    val status: String, // PENDING, ACTIVE, SUSPENDED, REJECTED
    val creditLimit: Long,
    val availableCredit: Long,
    val commissionRate: Double,
    val currency: String = "SAR"
)

@Serializable
data class AgencyProfileDto(
    val id: String,
    val name: String,
    val iataCode: String,
    val businessLicenseNumber: String,
    val contactPersonName: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String,
    val country: String,
    val status: String,
    val creditLimit: Long,
    val availableCredit: Long,
    val commissionRate: Double,
    val currency: String,
    val createdAt: String,
    val approvedAt: String?
)

@Serializable
data class UpdateAgencyProfileRequest(
    val contactPersonName: String,
    val phone: String,
    val address: String,
    val city: String
)

@Serializable
data class AgencyUserDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String, // ADMIN, AGENT, VIEWER
    val isActive: Boolean,
    val lastLoginAt: String?
)

@Serializable
data class InviteUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

// ========================================
// FLIGHT SEARCH DTOs
// ========================================

@Serializable
data class RoutesResponse(
    val routes: Map<String, List<String>>, // origin -> list of destinations
    val stations: List<StationDto>
)

@Serializable
data class StationDto(
    val code: String,
    val name: String,
    val nameAr: String,
    val city: String,
    val cityAr: String,
    val country: String,
    val countryAr: String,
    val timezone: String
)

@Serializable
data class FlightSearchRequest(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,
    val adults: Int,
    val children: Int = 0,
    val infants: Int = 0,
    val cabinClass: String = "ECONOMY",
    val directFlightsOnly: Boolean = false
)

@Serializable
data class FlightSearchResponse(
    val searchId: String,
    val outboundFlights: List<FlightOptionDto>,
    val returnFlights: List<FlightOptionDto>? = null,
    val currency: String
)

@Serializable
data class FlightOptionDto(
    val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val aircraft: String,
    val operatingCarrier: String,
    val availableSeats: Int,
    val fares: List<FareDto>
)

@Serializable
data class FareDto(
    val fareId: String,
    val fareFamily: String, // FLY, FLY_PLUS, FLY_MAX
    val price: Long,
    val currency: String,
    val cabinBaggage: String,
    val checkedBaggage: String,
    val seatSelection: String,
    val changePolicy: String,
    val refundPolicy: String,
    val mealIncluded: Boolean
)

@Serializable
data class LowFareCalendarResponse(
    val fares: Map<String, Long> // date -> lowest fare
)

// ========================================
// BOOKING DTOs
// ========================================

@Serializable
data class B2BBookingRequest(
    val searchId: String,
    val outboundFlightId: String,
    val outboundFareId: String,
    val returnFlightId: String? = null,
    val returnFareId: String? = null,
    val passengers: List<PassengerDto>,
    val contactEmail: String,
    val contactPhone: String,
    val paymentMethod: String, // CREDIT_ACCOUNT, CARD
    val clientReference: String? = null // Agency's internal reference
)

@Serializable
data class PassengerDto(
    val type: String, // ADULT, CHILD, INFANT
    val title: String, // MR, MRS, MS, MSTR, MISS
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String, // 2-letter ISO code
    val documentType: String, // PASSPORT, ID_CARD
    val documentNumber: String,
    val documentExpiry: String,
    val documentCountry: String
)

@Serializable
data class BookingConfirmation(
    val pnr: String,
    val bookingReference: String,
    val status: String,
    val totalAmount: Long,
    val currency: String,
    val agencyCommission: Long,
    val passengers: List<PassengerConfirmation>,
    val flights: List<FlightSegmentDto>,
    val createdAt: String
)

@Serializable
data class PassengerConfirmation(
    val passengerId: String,
    val name: String,
    val type: String,
    val ticketNumber: String?
)

@Serializable
data class FlightSegmentDto(
    val segmentId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val fareFamily: String,
    val cabinClass: String
)

@Serializable
data class PagedBookingsResponse(
    val bookings: List<BookingSummaryDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

@Serializable
data class BookingSummaryDto(
    val pnr: String,
    val bookingReference: String,
    val status: String, // CONFIRMED, TICKETED, CANCELLED, PENDING
    val passengerNames: String, // "John Doe, Jane Doe"
    val route: String, // "RUH -> JED"
    val departureDate: String,
    val totalAmount: Long,
    val currency: String,
    val createdAt: String,
    val clientReference: String?
)

@Serializable
data class BookingDetails(
    val pnr: String,
    val bookingReference: String,
    val status: String,
    val passengers: List<PassengerDetailDto>,
    val flights: List<FlightSegmentDetailDto>,
    val ancillaries: List<AncillaryDto>,
    val payment: PaymentDetailsDto,
    val contactEmail: String,
    val contactPhone: String,
    val clientReference: String?,
    val createdAt: String,
    val ticketedAt: String?,
    val agencyId: String,
    val agentEmail: String
)

@Serializable
data class PassengerDetailDto(
    val id: String,
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String,
    val documentType: String,
    val documentNumber: String,
    val documentExpiry: String,
    val ticketNumber: String?,
    val seatAssignments: List<SeatAssignmentDto>
)

@Serializable
data class SeatAssignmentDto(
    val segmentId: String,
    val seatNumber: String,
    val seatType: String // STANDARD, EXTRA_LEGROOM, EXIT_ROW
)

@Serializable
data class FlightSegmentDetailDto(
    val segmentId: String,
    val flightNumber: String,
    val origin: String,
    val originName: String,
    val destination: String,
    val destinationName: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val aircraft: String,
    val fareFamily: String,
    val cabinClass: String,
    val status: String // SCHEDULED, DEPARTED, ARRIVED, CANCELLED
)

@Serializable
data class AncillaryDto(
    val id: String,
    val type: String, // BAGGAGE, SEAT, MEAL, INSURANCE
    val description: String,
    val passengerId: String,
    val segmentId: String?,
    val price: Long,
    val currency: String,
    val status: String // CONFIRMED, PENDING
)

@Serializable
data class PaymentDetailsDto(
    val method: String,
    val totalAmount: Long,
    val currency: String,
    val basefare: Long,
    val taxes: Long,
    val fees: Long,
    val ancillaries: Long,
    val agencyCommission: Long,
    val paidAmount: Long,
    val paymentStatus: String // PAID, PENDING, PARTIALLY_PAID
)

@Serializable
data class CancelBookingRequest(
    val reason: String
)

@Serializable
data class CancellationResponse(
    val pnr: String,
    val status: String,
    val refundAmount: Long,
    val currency: String,
    val cancellationFee: Long,
    val message: String
)

// ========================================
// ANCILLARY DTOs
// ========================================

@Serializable
data class SeatMapResponse(
    val flightNumber: String,
    val aircraft: String,
    val rows: List<SeatRowDto>
)

@Serializable
data class SeatRowDto(
    val rowNumber: Int,
    val isExitRow: Boolean,
    val seats: List<SeatDto>
)

@Serializable
data class SeatDto(
    val seatNumber: String,
    val column: String,
    val type: String, // STANDARD, EXTRA_LEGROOM, EXIT_ROW, PREFERRED
    val isAvailable: Boolean,
    val price: Long,
    val currency: String
)

@Serializable
data class SeatReservationRequest(
    val pnr: String,
    val seatSelections: List<SeatSelectionDto>
)

@Serializable
data class SeatSelectionDto(
    val passengerId: String,
    val segmentId: String,
    val seatNumber: String
)

@Serializable
data class SeatReservationResponse(
    val success: Boolean,
    val reservedSeats: List<SeatAssignmentDto>,
    val totalCost: Long,
    val currency: String
)

@Serializable
data class MealDto(
    val id: String,
    val code: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val category: String, // STANDARD, VEGETARIAN, HALAL, KIDS
    val imageUrl: String?,
    val price: Long,
    val currency: String,
    val isAvailable: Boolean
)

@Serializable
data class AddAncillariesRequest(
    val ancillaries: List<AddAncillaryDto>
)

@Serializable
data class AddAncillaryDto(
    val type: String, // BAGGAGE, SEAT, MEAL
    val passengerId: String,
    val segmentId: String?,
    val itemId: String?, // meal id, baggage option id, etc.
    val quantity: Int = 1
)

// ========================================
// GROUP BOOKING DTOs
// ========================================

@Serializable
data class GroupBookingRequest(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String?,
    val passengerCount: Int,
    val purpose: String, // LEISURE, CORPORATE, SPORTS, RELIGIOUS, OTHER
    val notes: String?,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val flexibleDates: Boolean = false,
    val preferredTime: String? = null // MORNING, AFTERNOON, EVENING
)

@Serializable
data class GroupBookingRequestResponse(
    val requestId: String,
    val status: String,
    val message: String,
    val estimatedResponseTime: String // "2-3 business days"
)

@Serializable
data class GroupBookingRequestDto(
    val id: String,
    val origin: String,
    val originName: String,
    val destination: String,
    val destinationName: String,
    val departureDate: String,
    val returnDate: String?,
    val passengerCount: Int,
    val purpose: String,
    val notes: String?,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val status: String, // PENDING, QUOTED, ACCEPTED, REJECTED, EXPIRED
    val quotedPrice: Long?,
    val quoteCurrency: String?,
    val quoteValidUntil: String?,
    val quoteNotes: String?,
    val createdAt: String,
    val updatedAt: String,
    val assignedTo: String?
)

// ========================================
// CHARTER REQUEST DTOs
// ========================================

@Serializable
data class CharterRequest(
    val charterType: String, // HAJJ_UMRAH, SPORTS, CORPORATE, GOVERNMENT, ENTERTAINMENT, OTHER
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String?,
    val passengerCount: Int,
    val aircraftPreference: String?, // A320, A321, etc.
    val specialRequirements: String?,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val companyName: String?
)

@Serializable
data class CharterRequestResponse(
    val requestId: String,
    val status: String,
    val message: String,
    val estimatedResponseTime: String
)

@Serializable
data class CharterRequestDto(
    val id: String,
    val charterType: String,
    val origin: String,
    val originName: String,
    val destination: String,
    val destinationName: String,
    val departureDate: String,
    val returnDate: String?,
    val passengerCount: Int,
    val aircraftPreference: String?,
    val specialRequirements: String?,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val companyName: String?,
    val status: String, // PENDING, UNDER_REVIEW, QUOTED, ACCEPTED, REJECTED
    val quotedPrice: Long?,
    val quoteCurrency: String?,
    val quoteValidUntil: String?,
    val createdAt: String,
    val updatedAt: String
)

// ========================================
// REPORTS & ANALYTICS DTOs
// ========================================

@Serializable
data class AgencyStatsResponse(
    val totalBookings: Int,
    val confirmedBookings: Int,
    val cancelledBookings: Int,
    val totalRevenue: Long,
    val totalCommission: Long,
    val currency: String,
    val topRoutes: List<RouteStatDto>,
    val bookingsByMonth: Map<String, Int>,
    val revenueByMonth: Map<String, Long>
)

@Serializable
data class RouteStatDto(
    val origin: String,
    val destination: String,
    val bookingCount: Int,
    val revenue: Long
)

@Serializable
data class CommissionReportResponse(
    val month: Int,
    val year: Int,
    val totalBookings: Int,
    val totalRevenue: Long,
    val commissionRate: Double,
    val earnedCommission: Long,
    val paidCommission: Long,
    val pendingCommission: Long,
    val currency: String,
    val details: List<CommissionDetailDto>
)

@Serializable
data class CommissionDetailDto(
    val pnr: String,
    val bookingDate: String,
    val route: String,
    val bookingAmount: Long,
    val commissionAmount: Long,
    val status: String // PENDING, PAID
)

// ========================================
// UI HELPER EXTENSIONS
// ========================================

/**
 * Returns a display-friendly status string.
 */
fun String.toDisplayStatus(): String = when (this) {
    "PENDING" -> "Pending"
    "ACTIVE" -> "Active"
    "SUSPENDED" -> "Suspended"
    "REJECTED" -> "Rejected"
    "CONFIRMED" -> "Confirmed"
    "TICKETED" -> "Ticketed"
    "CANCELLED" -> "Cancelled"
    "QUOTED" -> "Quote Received"
    "ACCEPTED" -> "Accepted"
    "EXPIRED" -> "Expired"
    "UNDER_REVIEW" -> "Under Review"
    "PENDING_APPROVAL" -> "Pending Approval"
    else -> this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Returns a status color (as hex string) for UI.
 */
fun String.statusColor(): Long = when (this) {
    "ACTIVE", "CONFIRMED", "TICKETED", "ACCEPTED" -> 0xFF22C55E // Green
    "PENDING", "PENDING_APPROVAL", "UNDER_REVIEW" -> 0xFFF59E0B // Amber
    "QUOTED" -> 0xFF3B82F6 // Blue
    "CANCELLED", "REJECTED", "EXPIRED" -> 0xFFEF4444 // Red
    "SUSPENDED" -> 0xFF6B7280 // Gray
    else -> 0xFF6B7280 // Default gray
}

/**
 * Formats price with currency.
 */
fun Long.formatPrice(currency: String): String {
    val value = this / 100
    val formatted = value.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    return when (currency) {
        "SAR" -> "SAR $formatted"
        "USD" -> "$$formatted"
        "EUR" -> "€$formatted"
        "GBP" -> "£$formatted"
        else -> "$formatted $currency"
    }
}

/**
 * Formats flight duration.
 */
fun Int.formatDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
