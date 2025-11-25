package com.flyadeal.controller

import com.flyadeal.contract.api.ApiRoutes
import com.flyadeal.contract.model.RouteMap
import com.flyadeal.contract.model.Station
import com.flyadeal.service.FlightService
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger

/**
 * REST controller for configuration endpoints.
 * Provides route map and station data for the frontend.
 */
@Path(ApiRoutes.Config.BASE)
@Produces(MediaType.APPLICATION_JSON)
class ConfigController {

    @Inject
    lateinit var flightService: FlightService

    private val log = Logger.getLogger(ConfigController::class.java)

    /**
     * GET /api/v1/config/routes
     *
     * Returns the route map defining valid origin-destination pairs.
     * This data is cached for 24 hours.
     *
     * @return RouteMap containing all available routes
     */
    @GET
    @Path("/routes")
    fun getRoutes(): Uni<RouteMapResponse> {
        log.info("GET /config/routes")
        return Uni.createFrom().item {
            runBlocking {
                val routeMap = flightService.getRouteMap()
                RouteMapResponse.from(routeMap)
            }
        }
    }

    /**
     * GET /api/v1/config/stations
     *
     * Returns all available stations (airports).
     * This data is cached for 24 hours.
     *
     * @return List of all available stations
     */
    @GET
    @Path("/stations")
    fun getStations(): Uni<List<StationResponse>> {
        log.info("GET /config/stations")
        return Uni.createFrom().item {
            runBlocking {
                flightService.getStations().map { StationResponse.from(it) }
            }
        }
    }
}

/**
 * Response DTO for route map endpoint.
 * Serializes AirportCode values as strings for JSON.
 */
data class RouteMapResponse(
    val routes: Map<String, List<String>>
) {
    companion object {
        fun from(routeMap: RouteMap): RouteMapResponse {
            return RouteMapResponse(
                routes = routeMap.routes.mapKeys { it.key.value }
                    .mapValues { entry -> entry.value.map { it.value } }
            )
        }
    }
}

/**
 * Response DTO for station endpoint.
 */
data class StationResponse(
    val code: String,
    val name: String,
    val city: String,
    val country: String
) {
    companion object {
        fun from(station: Station): StationResponse {
            return StationResponse(
                code = station.code.value,
                name = station.name,
                city = station.city,
                country = station.country
            )
        }
    }
}
