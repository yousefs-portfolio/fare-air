package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.service.FlightService
import com.fairair.service.InvalidRouteException
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for flight search endpoints.
 */
@RestController
@RequestMapping(ApiRoutes.Search.BASE)
class SearchController(
    private val flightService: FlightService
) {
    private val log = LoggerFactory.getLogger(SearchController::class.java)

    /**
     * POST /api/v1/search
     *
     * Searches for available flights based on the provided criteria.
     * Results are cached for 5 minutes using the returned searchId.
     *
     * @param request Search criteria including origin, destination, date, and passengers
     * @return FlightResponse containing available flights and searchId
     */
    @PostMapping
    suspend fun searchFlights(@RequestBody request: FlightSearchRequestDto): ResponseEntity<Any> {
        log.info("POST /search: ${request.origin} -> ${request.destination} on ${request.departureDate}")

        return try {
            val searchRequest = request.toModel()
            val response = flightService.searchFlights(searchRequest)
            ResponseEntity.ok(FlightResponseDto.from(response))
        } catch (e: InvalidRouteException) {
            log.warn("Invalid route: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SearchErrorResponse("INVALID_ROUTE", e.message ?: "Invalid route"))
        } catch (e: IllegalArgumentException) {
            log.warn("Validation error: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SearchErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
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
 * Error response for search endpoint.
 */
data class SearchErrorResponse(
    val code: String,
    val message: String
)
