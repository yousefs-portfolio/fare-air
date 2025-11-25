package com.flyadeal.contract.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Request payload for flight search endpoint.
 * All fields are required and validated.
 */
@Serializable
data class FlightSearchRequest(
    val origin: AirportCode,
    val destination: AirportCode,
    val departureDate: LocalDate,
    val passengers: PassengerCounts
) {
    init {
        require(origin != destination) {
            "Origin and destination must be different: $origin"
        }
    }
}

/**
 * Response payload containing flight search results.
 * The searchId must be preserved and sent with the booking request.
 */
@Serializable
data class FlightResponse(
    val flights: List<Flight>,
    val searchId: String
) {
    /**
     * Returns true if no flights were found.
     */
    val isEmpty: Boolean
        get() = flights.isEmpty()

    /**
     * Returns the number of flights found.
     */
    val count: Int
        get() = flights.size

    /**
     * Returns the lowest priced flight across all fare families.
     */
    fun lowestPrice(): Money? = flights
        .flatMap { it.fareFamilies }
        .minByOrNull { it.price.amountMinor }
        ?.price
}

/**
 * Represents a single flight option with available fare families.
 */
@Serializable
data class Flight(
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val departureTime: Instant,
    val arrivalTime: Instant,
    val durationMinutes: Int,
    val aircraft: String,
    val fareFamilies: List<FareFamily>
) {
    init {
        require(flightNumber.isNotBlank()) {
            "Flight number must not be blank"
        }
        require(arrivalTime > departureTime) {
            "Arrival time must be after departure time"
        }
        require(durationMinutes > 0) {
            "Duration must be positive: $durationMinutes"
        }
        require(aircraft.isNotBlank()) {
            "Aircraft must not be blank"
        }
        require(fareFamilies.size == 3) {
            "Flight must have exactly 3 fare families, got ${fareFamilies.size}"
        }
    }

    /**
     * Returns the flight duration formatted as "Xh Ym".
     */
    fun formatDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }

    /**
     * Returns the fare family for a given code, if available.
     * @param code The fare family code to look up
     * @return The FareFamily or null if not found
     */
    fun getFareFamily(code: FareFamilyCode): FareFamily? =
        fareFamilies.find { it.code == code }

    /**
     * Returns the lowest priced fare family.
     */
    fun lowestFare(): FareFamily =
        fareFamilies.minBy { it.price.amountMinor }

    /**
     * Returns the highest priced fare family.
     */
    fun highestFare(): FareFamily =
        fareFamilies.maxBy { it.price.amountMinor }
}

/**
 * Summary of booked flight details for confirmation display.
 * Contains essential information without full fare details.
 */
@Serializable
data class FlightSummary(
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val departureTime: Instant,
    val fareFamily: FareFamilyCode
)
