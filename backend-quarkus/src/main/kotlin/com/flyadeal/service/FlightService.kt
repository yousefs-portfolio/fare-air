package com.flyadeal.service

import com.flyadeal.cache.CacheService
import com.flyadeal.client.NavitaireClient
import com.flyadeal.contract.model.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Service layer for flight-related operations.
 * Handles caching, validation, and orchestration between controllers and clients.
 */
@ApplicationScoped
class FlightService {

    @Inject
    lateinit var navitaireClient: NavitaireClient

    @Inject
    lateinit var cacheService: CacheService

    private val log = Logger.getLogger(FlightService::class.java)

    /**
     * Gets the route map with caching.
     * @return The RouteMap defining valid origin-destination pairs
     */
    suspend fun getRouteMap(): RouteMap {
        log.debug("Getting route map")
        return cacheService.getRouteMap {
            kotlinx.coroutines.runBlocking {
                navitaireClient.getRouteMap()
            }
        }
    }

    /**
     * Gets all stations with caching.
     * @return List of all available stations
     */
    suspend fun getStations(): List<Station> {
        log.debug("Getting stations")
        return cacheService.getStations {
            kotlinx.coroutines.runBlocking {
                navitaireClient.getStations()
            }
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
     * @throws IllegalArgumentException if the route is invalid
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
            ?: throw FareNotFoundException(fareFamily)
    }
}

/**
 * Exception thrown when an invalid route is requested.
 */
class InvalidRouteException(
    val origin: AirportCode,
    val destination: AirportCode
) : RuntimeException("Route not available: ${origin.value} -> ${destination.value}")

/**
 * Exception thrown when a search session has expired.
 */
class SearchExpiredException(
    val searchId: String
) : RuntimeException("Search session expired: $searchId")

/**
 * Exception thrown when a flight is not found in search results.
 */
class FlightNotFoundException(
    val flightNumber: String
) : RuntimeException("Flight not found: $flightNumber")

/**
 * Exception thrown when a fare family is not available.
 */
class FareNotFoundException(
    val fareFamily: FareFamilyCode
) : RuntimeException("Fare family not available: $fareFamily")
