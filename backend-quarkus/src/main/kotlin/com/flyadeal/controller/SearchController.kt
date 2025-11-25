package com.flyadeal.controller

import com.flyadeal.contract.api.ApiRoutes
import com.flyadeal.contract.model.*
import com.flyadeal.service.FlightService
import com.flyadeal.service.InvalidRouteException
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jboss.logging.Logger

/**
 * REST controller for flight search endpoints.
 */
@Path(ApiRoutes.Search.BASE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SearchController {

    @Inject
    lateinit var flightService: FlightService

    private val log = Logger.getLogger(SearchController::class.java)

    /**
     * POST /api/v1/search
     *
     * Searches for available flights based on the provided criteria.
     * Results are cached for 5 minutes using the returned searchId.
     *
     * @param request Search criteria including origin, destination, date, and passengers
     * @return FlightResponse containing available flights and searchId
     */
    @POST
    fun searchFlights(request: FlightSearchRequestDto): Uni<Response> {
        log.info("POST /search: ${request.origin} -> ${request.destination} on ${request.departureDate}")

        return Uni.createFrom().item {
            runBlocking {
                try {
                    val searchRequest = request.toModel()
                    val response = flightService.searchFlights(searchRequest)
                    Response.ok(FlightResponseDto.from(response)).build()
                } catch (e: InvalidRouteException) {
                    log.warn("Invalid route: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("INVALID_ROUTE", e.message ?: "Invalid route"))
                        .build()
                } catch (e: IllegalArgumentException) {
                    log.warn("Validation error: ${e.message}")
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
                        .build()
                } catch (e: Exception) {
                    log.error("Search failed", e)
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(ErrorResponse("INTERNAL_ERROR", "Search failed"))
                        .build()
                }
            }
        }
    }
}

/**
 * Request DTO for flight search.
 */
data class FlightSearchRequestDto(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val passengers: PassengerCountsDto
) {
    fun toModel(): FlightSearchRequest {
        return FlightSearchRequest(
            origin = AirportCode(origin),
            destination = AirportCode(destination),
            departureDate = LocalDate.parse(departureDate),
            passengers = passengers.toModel()
        )
    }
}

/**
 * DTO for passenger counts.
 */
data class PassengerCountsDto(
    val adults: Int,
    val children: Int,
    val infants: Int
) {
    fun toModel(): PassengerCounts {
        return PassengerCounts(
            adults = adults,
            children = children,
            infants = infants
        )
    }
}

/**
 * Response DTO for flight search.
 */
data class FlightResponseDto(
    val flights: List<FlightDto>,
    val searchId: String
) {
    companion object {
        fun from(response: FlightResponse): FlightResponseDto {
            return FlightResponseDto(
                flights = response.flights.map { FlightDto.from(it) },
                searchId = response.searchId
            )
        }
    }
}

/**
 * DTO for individual flight.
 */
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val durationFormatted: String,
    val aircraft: String,
    val fareFamilies: List<FareFamilyDto>
) {
    companion object {
        fun from(flight: Flight): FlightDto {
            return FlightDto(
                flightNumber = flight.flightNumber,
                origin = flight.origin.value,
                destination = flight.destination.value,
                departureTime = flight.departureTime.toString(),
                arrivalTime = flight.arrivalTime.toString(),
                durationMinutes = flight.durationMinutes,
                durationFormatted = flight.formatDuration(),
                aircraft = flight.aircraft,
                fareFamilies = flight.fareFamilies.map { FareFamilyDto.from(it) }
            )
        }
    }
}

/**
 * DTO for fare family.
 */
data class FareFamilyDto(
    val code: String,
    val name: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val inclusions: FareInclusionsDto
) {
    companion object {
        fun from(fareFamily: FareFamily): FareFamilyDto {
            return FareFamilyDto(
                code = fareFamily.code.name,
                name = fareFamily.name,
                priceMinor = fareFamily.price.amountMinor,
                priceFormatted = fareFamily.price.formatAmount(),
                currency = fareFamily.price.currency.name,
                inclusions = FareInclusionsDto.from(fareFamily.inclusions)
            )
        }
    }
}

/**
 * DTO for fare inclusions.
 */
data class FareInclusionsDto(
    val carryOnBag: String,
    val checkedBag: String?,
    val seatSelection: String,
    val changePolicy: String,
    val cancellationPolicy: String,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
) {
    companion object {
        fun from(inclusions: FareInclusions): FareInclusionsDto {
            return FareInclusionsDto(
                carryOnBag = inclusions.carryOnBag.formatDisplay(),
                checkedBag = inclusions.checkedBag?.formatDisplay(),
                seatSelection = inclusions.seatSelection.displayName,
                changePolicy = inclusions.changePolicy.formatDisplay(),
                cancellationPolicy = inclusions.cancellationPolicy.formatDisplay(),
                priorityBoarding = inclusions.priorityBoarding,
                loungeAccess = inclusions.loungeAccess
            )
        }
    }
}

/**
 * Generic error response.
 */
data class ErrorResponse(
    val code: String,
    val message: String
)
