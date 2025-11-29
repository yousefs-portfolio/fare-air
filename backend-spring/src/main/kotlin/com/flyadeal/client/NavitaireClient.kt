package com.flyadeal.client

import com.flyadeal.contract.model.*

/**
 * Client interface for Navitaire GDS integration.
 * Defines the contract for fetching flight data, creating bookings, etc.
 *
 * Two implementations:
 * - MockNavitaireClient: Returns simulated data for development/testing
 * - RealNavitaireClient: Connects to actual Navitaire APIs (future implementation)
 */
interface NavitaireClient {

    /**
     * Fetches the route map defining valid origin-destination pairs.
     * @return RouteMap containing all available routes
     */
    suspend fun getRouteMap(): RouteMap

    /**
     * Fetches all stations (airports) in the network.
     * @return List of all available stations
     */
    suspend fun getStations(): List<Station>

    /**
     * Searches for available flights based on the given criteria.
     * @param request The search criteria
     * @return FlightResponse containing available flights
     */
    suspend fun searchFlights(request: FlightSearchRequest): FlightResponse

    /**
     * Creates a booking for the selected flight and passengers.
     * @param request The booking details
     * @return BookingConfirmation with PNR and booking details
     */
    suspend fun createBooking(request: BookingRequest): BookingConfirmation

    /**
     * Retrieves an existing booking by PNR.
     * @param pnr The 6-character PNR code
     * @return BookingConfirmation or null if not found
     */
    suspend fun getBooking(pnr: String): BookingConfirmation?
}
