package com.flyadeal.controller

import com.flyadeal.contract.api.ApiRoutes
import com.flyadeal.contract.model.*
import com.flyadeal.service.*
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jboss.logging.Logger

/**
 * REST controller for booking endpoints.
 */
@Path(ApiRoutes.Booking.BASE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BookingController {

    @Inject
    lateinit var bookingService: BookingService

    private val log = Logger.getLogger(BookingController::class.java)

    /**
     * POST /api/v1/booking
     *
     * Creates a new booking for the selected flight and passengers.
     *
     * @param request Booking details including passengers and payment
     * @return BookingConfirmation with PNR and booking details
     */
    @POST
    fun createBooking(request: BookingRequestDto): Uni<Response> {
        log.info("POST /booking: flight=${request.flightNumber}, passengers=${request.passengers.size}")

        return Uni.createFrom().item {
            runBlocking {
                try {
                    val bookingRequest = request.toModel()
                    val confirmation = bookingService.createBooking(bookingRequest)
                    Response.status(Response.Status.CREATED)
                        .entity(BookingConfirmationDto.from(confirmation))
                        .build()
                } catch (e: SearchExpiredException) {
                    log.warn("Search expired: ${e.searchId}")
                    Response.status(Response.Status.GONE)
                        .entity(ErrorResponse("SEARCH_EXPIRED", "Search session has expired. Please search again."))
                        .build()
                } catch (e: FlightNotFoundException) {
                    log.warn("Flight not found: ${e.flightNumber}")
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(ErrorResponse("FLIGHT_NOT_FOUND", "Selected flight is no longer available."))
                        .build()
                } catch (e: FareNotFoundException) {
                    log.warn("Fare not found: ${e.fareFamily}")
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(ErrorResponse("FARE_NOT_FOUND", "Selected fare is no longer available."))
                        .build()
                } catch (e: PassengerValidationException) {
                    log.warn("Passenger validation failed: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("VALIDATION_ERROR", e.message))
                        .build()
                } catch (e: AncillaryValidationException) {
                    log.warn("Ancillary validation failed: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("VALIDATION_ERROR", e.message))
                        .build()
                } catch (e: PaymentValidationException) {
                    log.warn("Payment validation failed: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("PAYMENT_ERROR", e.message))
                        .build()
                } catch (e: IllegalArgumentException) {
                    log.warn("Validation error: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
                        .build()
                } catch (e: Exception) {
                    log.error("Booking failed", e)
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(ErrorResponse("BOOKING_FAILED", "Unable to complete booking"))
                        .build()
                }
            }
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
    @GET
    @Path("/{pnr}")
    fun getBooking(@PathParam("pnr") pnr: String): Uni<Response> {
        log.info("GET /booking/$pnr")

        return Uni.createFrom().item {
            runBlocking {
                val confirmation = bookingService.getBooking(pnr)
                if (confirmation != null) {
                    Response.ok(BookingConfirmationDto.from(confirmation)).build()
                } else {
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(ErrorResponse("NOT_FOUND", "Booking not found for PNR: $pnr"))
                        .build()
                }
            }
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
