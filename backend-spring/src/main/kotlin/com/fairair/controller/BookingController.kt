package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.exception.*
import com.fairair.service.BookingService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for booking endpoints.
 */
@RestController
@RequestMapping(ApiRoutes.Booking.BASE)
class BookingController(
    private val bookingService: BookingService
) {
    private val log = LoggerFactory.getLogger(BookingController::class.java)

    /**
     * POST /api/v1/booking
     *
     * Creates a new booking for the selected flight and passengers.
     * If user is authenticated, the booking is associated with their account.
     *
     * @param request Booking details including passengers and payment
     * @return BookingConfirmation with PNR and booking details
     */
    @PostMapping
    suspend fun createBooking(
        @RequestBody request: BookingRequestDto
    ): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("POST /booking: flight=${request.flightNumber}, passengers=${request.passengers.size}, userId=$userId")

        return try {
            val bookingRequest = request.toModel()
            val confirmation = bookingService.createBooking(bookingRequest, userId)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingConfirmationDto.from(confirmation))
        } catch (e: SearchExpiredException) {
            log.warn("Search expired: ${e.searchId}")
            ResponseEntity.status(HttpStatus.GONE)
                .body(BookingErrorResponse("SEARCH_EXPIRED", "Search session has expired. Please search again."))
        } catch (e: FlightNotFoundException) {
            log.warn("Flight not found: ${e.flightNumber}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BookingErrorResponse("FLIGHT_NOT_FOUND", "Selected flight is no longer available."))
        } catch (e: FareNotFoundException) {
            log.warn("Fare not found: ${e.fareFamily}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BookingErrorResponse("FARE_NOT_FOUND", "Selected fare is no longer available."))
        } catch (e: PassengerValidationException) {
            log.warn("Passenger validation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BookingErrorResponse("VALIDATION_ERROR", e.message))
        } catch (e: AncillaryValidationException) {
            log.warn("Ancillary validation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BookingErrorResponse("VALIDATION_ERROR", e.message))
        } catch (e: PaymentValidationException) {
            log.warn("Payment validation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BookingErrorResponse("PAYMENT_ERROR", e.message))
        } catch (e: IllegalArgumentException) {
            log.warn("Validation error: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        }
    }

    /**
     * GET /api/v1/booking/{pnr}
     *
     * Retrieves an existing booking by PNR.
     *
     * @param pnr The 6-character PNR code
     * @return BookingConfirmation or 404 if not found
     */
    @GetMapping("/{pnr}")
    suspend fun getBooking(@PathVariable pnr: String): ResponseEntity<Any> {
        log.info("GET /booking/$pnr")

        val confirmation = bookingService.getBooking(pnr)
        return if (confirmation != null) {
            ResponseEntity.ok(BookingConfirmationDto.from(confirmation))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BookingErrorResponse("NOT_FOUND", "Booking not found for PNR: $pnr"))
        }
    }
    
    /**
     * GET /api/v1/booking/user/me
     *
     * Retrieves all bookings for the authenticated user.
     *
     * @return List of bookings for this user
     */
    @GetMapping("/user/me")
    suspend fun getMyBookings(): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("GET /booking/user/me: userId=$userId")
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BookingErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        val bookings = bookingService.getBookingsByUser(userId)
        return ResponseEntity.ok(bookings.map { BookingConfirmationDto.from(it) })
    }
    
    /**
     * Helper function to extract user ID from the reactive security context.
     * Returns null if not authenticated.
     */
    private suspend fun getCurrentUserId(): String? {
        return try {
            val authentication = ReactiveSecurityContextHolder.getContext()
                .map { it.authentication }
                .awaitSingleOrNull()
            authentication?.principal as? String
        } catch (e: Exception) {
            log.debug("Could not get current user: ${e.message}")
            null
        }
    }
}

/**
 * Request DTO for booking creation.
 */
data class BookingRequestDto(
    val searchId: String,
    val flightNumber: String,
    val fareFamily: String,
    val passengers: List<PassengerDto>,
    val ancillaries: List<AncillaryDto>,
    val contactEmail: String,
    val payment: PaymentDetailsDto
) {
    fun toModel(): BookingRequest {
        return BookingRequest(
            searchId = searchId,
            flightNumber = flightNumber,
            fareFamily = FareFamilyCode.valueOf(fareFamily),
            passengers = passengers.map { it.toModel() },
            ancillaries = ancillaries.map { it.toModel() },
            contactEmail = contactEmail,
            payment = payment.toModel()
        )
    }
}

/**
 * DTO for passenger data in booking request.
 */
data class PassengerDto(
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: String,
    val documentId: String
) {
    fun toModel(): Passenger {
        return Passenger(
            type = PassengerType.valueOf(type),
            title = Title.valueOf(title),
            firstName = firstName,
            lastName = lastName,
            nationality = nationality,
            dateOfBirth = LocalDate.parse(dateOfBirth),
            documentId = documentId
        )
    }
}

/**
 * DTO for ancillary in booking request.
 */
data class AncillaryDto(
    val type: String,
    val passengerIndex: Int,
    val priceMinor: Long,
    val currency: String
) {
    fun toModel(): Ancillary {
        return Ancillary(
            type = AncillaryType.valueOf(type),
            passengerIndex = passengerIndex,
            price = Money(priceMinor, Currency.valueOf(currency))
        )
    }
}

/**
 * DTO for payment details in booking request.
 */
data class PaymentDetailsDto(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmountMinor: Long,
    val currency: String
) {
    fun toModel(): PaymentDetails {
        return PaymentDetails(
            cardholderName = cardholderName,
            cardNumberLast4 = cardNumberLast4,
            totalAmount = Money(totalAmountMinor, Currency.valueOf(currency))
        )
    }
}

/**
 * Response DTO for booking confirmation.
 */
data class BookingConfirmationDto(
    val pnr: String,
    val bookingReference: String,
    val flight: FlightSummaryDto,
    val passengers: List<PassengerSummaryDto>,
    val totalPaidMinor: Long,
    val totalPaidFormatted: String,
    val currency: String,
    val createdAt: String
) {
    companion object {
        fun from(confirmation: BookingConfirmation): BookingConfirmationDto {
            return BookingConfirmationDto(
                pnr = confirmation.pnr.value,
                bookingReference = confirmation.bookingReference,
                flight = FlightSummaryDto.from(confirmation.flight),
                passengers = confirmation.passengers.map { PassengerSummaryDto.from(it) },
                totalPaidMinor = confirmation.totalPaid.amountMinor,
                totalPaidFormatted = confirmation.totalPaid.formatDisplay(),
                currency = confirmation.totalPaid.currency.name,
                createdAt = confirmation.createdAt.toString()
            )
        }
    }
}

/**
 * DTO for flight summary in confirmation.
 */
data class FlightSummaryDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val fareFamily: String
) {
    companion object {
        fun from(summary: FlightSummary): FlightSummaryDto {
            return FlightSummaryDto(
                flightNumber = summary.flightNumber,
                origin = summary.origin.value,
                destination = summary.destination.value,
                departureTime = summary.departureTime.toString(),
                fareFamily = summary.fareFamily.name
            )
        }
    }
}

/**
 * DTO for passenger summary in confirmation.
 */
data class PassengerSummaryDto(
    val fullName: String,
    val type: String
) {
    companion object {
        fun from(summary: PassengerSummary): PassengerSummaryDto {
            return PassengerSummaryDto(
                fullName = summary.fullName,
                type = summary.type.name
            )
        }
    }
}

/**
 * Error response for booking endpoint.
 */
data class BookingErrorResponse(
    val code: String,
    val message: String
)
