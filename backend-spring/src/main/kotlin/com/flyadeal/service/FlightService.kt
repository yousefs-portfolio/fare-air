package com.flyadeal.service

import com.flyadeal.cache.CacheService
import com.flyadeal.client.NavitaireClient
import com.flyadeal.contract.model.*
import com.flyadeal.exception.FareNotFoundException
import com.flyadeal.exception.FlightNotFoundException
import com.flyadeal.exception.SearchExpiredException
import com.flyadeal.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service layer for flight-related operations.
 * Handles caching, validation, and orchestration between controllers and clients.
 */
@Service
class FlightService(
    private val navitaireClient: NavitaireClient,
    private val cacheService: CacheService
) {
    private val log = LoggerFactory.getLogger(FlightService::class.java)

    /**
     * Gets the route map with caching.
     * Uses suspend-aware cache retrieval to avoid blocking Netty threads.
     * @return The RouteMap defining valid origin-destination pairs
     */
    suspend fun getRouteMap(): RouteMap {
        log.debug("Getting route map")
        return cacheService.getRouteMapSuspend {
            navitaireClient.getRouteMap()
        }
    }

    /**
     * Gets all stations with caching.
     * Uses suspend-aware cache retrieval to avoid blocking Netty threads.
     * @return List of all available stations
     */
    suspend fun getStations(): List<Station> {
        log.debug("Getting stations")
        return cacheService.getStationsSuspend {
            navitaireClient.getStations()
        }
    }

    /**
     * Gets a station by its airport code.
     * @param code The airport code
     * @return The Station or null if not found
     */
    suspend fun getStation(code: AirportCode): Station? {
        return getStations().find { it.code == code }
    }

    /**
     * Searches for flights and caches the result.
     * @param request The search criteria
     * @return FlightResponse containing available flights
     * @throws ValidationException if the route is invalid
     */
    suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        log.info("Searching flights: ${request.origin} -> ${request.destination}")

        val routeMap = getRouteMap()
        if (!routeMap.isValidRoute(request.origin, request.destination)) {
            log.warn("Invalid route requested: ${request.origin} -> ${request.destination}")
            throw InvalidRouteException(request.origin, request.destination)
        }

        val response = navitaireClient.searchFlights(request)
        cacheService.cacheSearchResult(response.searchId, response)

        log.info("Found ${response.count} flights for search ${response.searchId}")
        return response
    }

    /**
     * Retrieves a cached search result by ID.
     * @param searchId The search identifier
     * @return The cached FlightResponse or null if expired/not found
     */
    fun getCachedSearch(searchId: String): FlightResponse? {
        return cacheService.getSearchResult(searchId)
    }

    /**
     * Validates that a search session is still valid and the selected flight exists.
     * @param searchId The search identifier
     * @param flightNumber The selected flight number
     * @param fareFamily The selected fare family
     * @return The selected FareFamily if valid
     * @throws SearchExpiredException if the search is no longer cached
     * @throws FlightNotFoundException if the flight is not in the search results
     * @throws FareNotFoundException if the fare family is not available
     */
    fun validateSelection(
        searchId: String,
        flightNumber: String,
        fareFamily: FareFamilyCode
    ): FareFamily {
        val cachedSearch = getCachedSearch(searchId)
            ?: throw SearchExpiredException(searchId)

        val flight = cachedSearch.flights.find { it.flightNumber == flightNumber }
            ?: throw FlightNotFoundException(flightNumber)

        return flight.getFareFamily(fareFamily)
            ?: throw FareNotFoundException(fareFamily.name)
    }
}

/**
 * Exception thrown when an invalid route is requested.
 */
class InvalidRouteException(
    val origin: AirportCode,
    val destination: AirportCode
) : RuntimeException("Route not available: ${origin.value} -> ${destination.value}")
