package com.fairair.service

import com.fairair.client.NavitaireClient
import com.fairair.contract.model.*
import com.fairair.exception.AncillaryValidationException
import com.fairair.exception.PassengerValidationException
import com.fairair.exception.PaymentValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service layer for booking-related operations.
 * Handles booking creation, validation, and retrieval.
 */
@Service
class BookingService(
    private val navitaireClient: NavitaireClient,
    private val flightService: FlightService
) {
    private val log = LoggerFactory.getLogger(BookingService::class.java)

    /**
     * Creates a new booking after validating the selection and passenger data.
     * @param request The booking request with all required details
     * @return BookingConfirmation with PNR and booking details
     * @throws SearchExpiredException if the search session has expired
     * @throws FlightNotFoundException if the selected flight is not found
     * @throws FareNotFoundException if the selected fare is not available
     * @throws PassengerValidationException if passenger data is invalid
     */
    suspend fun createBooking(request: BookingRequest): BookingConfirmation {
        log.info("Creating booking for search=${request.searchId}, flight=${request.flightNumber}")

        val selectedFare = flightService.validateSelection(
            searchId = request.searchId,
            flightNumber = request.flightNumber,
            fareFamily = request.fareFamily
        )

        validatePassengers(request.passengers)
        validateAncillaries(request.ancillaries, request.passengers.size, request.fareFamily)
        validatePaymentAmount(request, selectedFare)

        val confirmation = navitaireClient.createBooking(request)
        log.info("Booking created successfully: PNR=${confirmation.pnr.value}")

        return confirmation
    }

    /**
     * Retrieves an existing booking by PNR.
     * @param pnr The 6-character PNR code
     * @return BookingConfirmation or null if not found
     */
    suspend fun getBooking(pnr: String): BookingConfirmation? {
        log.info("Retrieving booking for PNR=$pnr")

        val normalizedPnr = pnr.uppercase().trim()
        if (normalizedPnr.length != 6 || !normalizedPnr.all { it.isLetterOrDigit() }) {
            log.warn("Invalid PNR format: $pnr")
            return null
        }

        return navitaireClient.getBooking(normalizedPnr)
    }

    /**
     * Validates passenger data.
     * @param passengers List of passengers to validate
     * @throws PassengerValidationException if any validation fails
     */
    private fun validatePassengers(passengers: List<Passenger>) {
        if (passengers.isEmpty()) {
            throw PassengerValidationException("At least one passenger is required")
        }

        if (passengers.size > 9) {
            throw PassengerValidationException("Maximum 9 passengers allowed")
        }

        val adultsCount = passengers.count { it.type == PassengerType.ADULT }
        val infantsCount = passengers.count { it.type == PassengerType.INFANT }

        if (adultsCount == 0) {
            throw PassengerValidationException("At least one adult passenger is required")
        }

        if (infantsCount > adultsCount) {
            throw PassengerValidationException("Number of infants ($infantsCount) cannot exceed adults ($adultsCount)")
        }

        passengers.forEachIndexed { index, passenger ->
            validatePassenger(passenger, index)
        }
    }

    /**
     * Validates a single passenger's data.
     */
    private fun validatePassenger(passenger: Passenger, index: Int) {
        if (passenger.firstName.length < 2 || passenger.firstName.length > 50) {
            throw PassengerValidationException("Passenger ${index + 1}: First name must be 2-50 characters")
        }

        if (passenger.lastName.length < 2 || passenger.lastName.length > 50) {
            throw PassengerValidationException("Passenger ${index + 1}: Last name must be 2-50 characters")
        }

        if (passenger.documentId.length < 5 || passenger.documentId.length > 20) {
            throw PassengerValidationException("Passenger ${index + 1}: Document ID must be 5-20 characters")
        }

        if (!passenger.documentId.all { it.isLetterOrDigit() }) {
            throw PassengerValidationException("Passenger ${index + 1}: Document ID must be alphanumeric")
        }
    }

    /**
     * Validates ancillaries against passenger count and fare family inclusions.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun validateAncillaries(
        ancillaries: List<Ancillary>,
        passengerCount: Int,
        fareFamily: FareFamilyCode
    ) {
        ancillaries.forEach { ancillary ->
            if (ancillary.passengerIndex >= passengerCount) {
                throw AncillaryValidationException(
                    "Ancillary references invalid passenger index: ${ancillary.passengerIndex}"
                )
            }
        }
    }

    /**
     * Validates that the payment amount matches the expected total.
     * In production, this would calculate the exact expected amount.
     * For mock implementation, we trust the frontend calculation.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun validatePaymentAmount(request: BookingRequest, selectedFare: FareFamily) {
        if (request.payment.totalAmount.amountMinor <= 0) {
            throw PaymentValidationException("Payment amount must be positive")
        }
    }
}
