package com.flyadeal.contract.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents an airport in the flyadeal route network.
 * Each station is uniquely identified by its IATA airport code.
 */
@Serializable
data class Station(
    val code: AirportCode,
    val name: String,
    val city: String,
    val country: String
) {
    init {
        require(name.isNotBlank()) { "Airport name must not be blank" }
        require(city.isNotBlank()) { "City name must not be blank" }
        require(country.isNotBlank()) { "Country name must not be blank" }
    }
}

/**
 * Value class representing a 3-letter IATA airport code.
 * All codes must be exactly 3 uppercase letters (e.g., "JED", "RUH", "DMM").
 */
@Serializable
@JvmInline
value class AirportCode(val value: String) {
    init {
        require(value.length == 3) {
            "Airport code must be exactly 3 characters, got ${value.length}: '$value'"
        }
        require(value.all { it.isUpperCase() && it.isLetter() }) {
            "Airport code must be 3 uppercase letters: '$value'"
        }
    }

    override fun toString(): String = value
}

/**
 * Defines valid origin-destination pairs for flight search.
 * Routes are unidirectional: a route from A to B does not imply B to A exists.
 */
@Serializable
data class RouteMap(
    val routes: Map<AirportCode, List<AirportCode>>
) {
    init {
        require(routes.isNotEmpty()) { "RouteMap must contain at least one route" }
    }

    /**
     * Returns the list of valid destinations for a given origin.
     * @param origin The departure airport code
     * @return List of valid destination airport codes, or empty if origin not found
     */
    fun getDestinationsFor(origin: AirportCode): List<AirportCode> =
        routes[origin] ?: emptyList()

    /**
     * Checks if a route exists between the given origin and destination.
     * @param origin The departure airport code
     * @param destination The arrival airport code
     * @return true if the route is valid, false otherwise
     */
    fun isValidRoute(origin: AirportCode, destination: AirportCode): Boolean =
        routes[origin]?.contains(destination) == true

    /**
     * Returns all unique origins in the route map.
     */
    val allOrigins: Set<AirportCode>
        get() = routes.keys
}
