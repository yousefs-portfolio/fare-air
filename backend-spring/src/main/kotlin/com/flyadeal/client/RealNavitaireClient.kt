package com.flyadeal.client

import com.flyadeal.contract.model.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Real implementation of NavitaireClient that connects to actual Navitaire APIs.
 *
 * Activated when flyadeal.provider=real.
 *
 * NOTE: This is a placeholder implementation. The actual Navitaire API integration
 * should be implemented when real connectivity is required.
 */
@Service
@ConditionalOnProperty(
    name = ["flyadeal.provider"],
    havingValue = "real"
)
class RealNavitaireClient : NavitaireClient {

    private val log = LoggerFactory.getLogger(RealNavitaireClient::class.java)

    override suspend fun getRouteMap(): RouteMap {
        log.warn("RealNavitaireClient.getRouteMap() called but not implemented")
        throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
    }

    override suspend fun getStations(): List<Station> {
        log.warn("RealNavitaireClient.getStations() called but not implemented")
        throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
    }

    override suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        log.warn("RealNavitaireClient.searchFlights() called but not implemented")
        throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
    }

    override suspend fun createBooking(request: BookingRequest): BookingConfirmation {
        log.warn("RealNavitaireClient.createBooking() called but not implemented")
        throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
    }

    override suspend fun getBooking(pnr: String): BookingConfirmation? {
        log.warn("RealNavitaireClient.getBooking() called but not implemented")
        throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
    }
}
