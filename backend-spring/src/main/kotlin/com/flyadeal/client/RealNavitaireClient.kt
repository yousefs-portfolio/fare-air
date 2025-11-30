package com.flyadeal.client

import com.flyadeal.config.FlyadealProperties
import com.flyadeal.contract.model.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
 *
 * Security: All external API calls are wrapped with configurable timeouts to prevent
 * resource exhaustion and hanging connections.
 */
@Service
@ConditionalOnProperty(
    name = ["flyadeal.provider"],
    havingValue = "real"
)
class RealNavitaireClient(
    private val config: FlyadealProperties
) : NavitaireClient {

    private val log = LoggerFactory.getLogger(RealNavitaireClient::class.java)

    /**
     * Wraps an external API call with configurable timeout.
     * Throws TimeoutException if the call takes too long.
     */
    private suspend fun <T> withApiTimeout(operationName: String, block: suspend () -> T): T {
        return try {
            withTimeout(config.timeout.requestMs) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            log.error("API timeout for operation: $operationName after ${config.timeout.requestMs}ms")
            throw ApiTimeoutException("External API call timed out for: $operationName")
        }
    }

    override suspend fun getRouteMap(): RouteMap {
        return withApiTimeout("getRouteMap") {
            log.warn("RealNavitaireClient.getRouteMap() called but not implemented")
            throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
        }
    }

    override suspend fun getStations(): List<Station> {
        return withApiTimeout("getStations") {
            log.warn("RealNavitaireClient.getStations() called but not implemented")
            throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
        }
    }

    override suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        return withApiTimeout("searchFlights") {
            log.warn("RealNavitaireClient.searchFlights() called but not implemented")
            throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
        }
    }

    override suspend fun createBooking(request: BookingRequest): BookingConfirmation {
        return withApiTimeout("createBooking") {
            log.warn("RealNavitaireClient.createBooking() called but not implemented")
            throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
        }
    }

    override suspend fun getBooking(pnr: String): BookingConfirmation? {
        return withApiTimeout("getBooking") {
            log.warn("RealNavitaireClient.getBooking() called but not implemented")
            throw NotImplementedError("Real Navitaire client is not yet implemented. Set flyadeal.provider=mock to use mock data.")
        }
    }
}

/**
 * Exception thrown when an external API call times out.
 */
class ApiTimeoutException(message: String) : RuntimeException(message)
