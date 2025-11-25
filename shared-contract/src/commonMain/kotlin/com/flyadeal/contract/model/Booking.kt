package com.flyadeal.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Request payload for booking creation.
 * Links to a previous search and contains all passenger and payment information.
 */
@Serializable
data class BookingRequest(
    val searchId: String,
    val flightNumber: String,
    val fareFamily: FareFamilyCode,
    val passengers: List<Passenger>,
    val ancillaries: List<Ancillary>,
    val contactEmail: String,
    val payment: PaymentDetails
) {
    init {
        require(searchId.isNotBlank()) {
            "Search ID must not be blank"
        }
        require(flightNumber.isNotBlank()) {
            "Flight number must not be blank"
        }
        require(passengers.isNotEmpty()) {
            "At least one passenger is required"
        }
        require(passengers.size <= 9) {
            "Maximum 9 passengers allowed, got ${passengers.size}"
        }
        require(isValidEmail(contactEmail)) {
            "Invalid email format: $contactEmail"
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
        )

        private fun isValidEmail(email: String): Boolean =
            EMAIL_REGEX.matches(email)
    }
}

/**
 * Response payload for successful booking.
 * Contains the PNR and all confirmation details.
 */
@Serializable
data class BookingConfirmation(
    val pnr: PnrCode,
    val bookingReference: String,
    val flight: FlightSummary,
    val passengers: List<PassengerSummary>,
    val totalPaid: Money,
    val createdAt: Instant
) {
    /**
     * Returns a display-friendly summary string.
     */
    fun summaryDisplay(): String =
        "Booking ${pnr.value}: ${flight.flightNumber} on ${flight.departureTime}"
}

/**
 * Value class representing a 6-character Passenger Name Record (PNR) code.
 * Used as the primary booking reference for customers.
 */
@Serializable
@JvmInline
value class PnrCode(val value: String) {
    init {
        require(value.length == 6) {
            "PNR must be exactly 6 characters, got ${value.length}: '$value'"
        }
        require(value.all { it.isUpperCase() || it.isDigit() }) {
            "PNR must be alphanumeric uppercase: '$value'"
        }
    }

    override fun toString(): String = value

    companion object {
        /**
         * Generates a random PNR code.
         * Uses letters and numbers excluding ambiguous characters (0, O, I, 1).
         */
        fun generate(): PnrCode {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val pnr = (1..6).map { chars.random() }.joinToString("")
            return PnrCode(pnr)
        }
    }
}

/**
 * Additional service that can be added to a booking.
 */
@Serializable
data class Ancillary(
    val type: AncillaryType,
    val passengerIndex: Int,
    val price: Money
) {
    init {
        require(passengerIndex >= 0) {
            "Passenger index must be non-negative: $passengerIndex"
        }
    }
}

/**
 * Types of ancillary services available for purchase.
 */
@Serializable
enum class AncillaryType(val displayName: String) {
    /**
     * Additional checked baggage allowance.
     */
    CHECKED_BAG("Checked Bag")
}

/**
 * Payment details for the booking.
 * For mock implementation, contains summary information only.
 */
@Serializable
data class PaymentDetails(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmount: Money
) {
    init {
        require(cardholderName.isNotBlank()) {
            "Cardholder name must not be blank"
        }
        require(cardNumberLast4.length == 4 && cardNumberLast4.all { it.isDigit() }) {
            "Card last 4 must be exactly 4 digits: '$cardNumberLast4'"
        }
    }

    /**
     * Returns a masked card display string (e.g., "**** **** **** 1234").
     */
    fun maskedCardDisplay(): String = "**** **** **** $cardNumberLast4"
}

/**
 * Full payment form data for processing.
 * Used in the frontend for form handling before submission.
 * Note: Not serializable - only used locally in the frontend.
 */
data class PaymentFormData(
    val cardholderName: String,
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String
) {
    /**
     * Validates the card number using Luhn algorithm.
     * @return true if the card number passes Luhn check
     */
    fun isValidLuhn(): Boolean {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length !in 13..19) return false

        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    /**
     * Validates the expiry date is not in the past.
     * @param currentMonth Current month (1-12)
     * @param currentYear Current year (4 digits)
     * @return true if the card is not expired
     */
    fun isNotExpired(currentMonth: Int, currentYear: Int): Boolean {
        val expMonth = expiryMonth.toIntOrNull() ?: return false
        val expYear = expiryYear.toIntOrNull()?.let {
            if (it < 100) 2000 + it else it
        } ?: return false

        return when {
            expYear > currentYear -> true
            expYear == currentYear && expMonth >= currentMonth -> true
            else -> false
        }
    }

    /**
     * Converts to PaymentDetails for API submission.
     * @param totalAmount The total amount to charge
     * @return PaymentDetails with masked card number
     */
    fun toPaymentDetails(totalAmount: Money): PaymentDetails {
        val last4 = cardNumber.filter { it.isDigit() }.takeLast(4)
        return PaymentDetails(
            cardholderName = cardholderName,
            cardNumberLast4 = last4,
            totalAmount = totalAmount
        )
    }
}
