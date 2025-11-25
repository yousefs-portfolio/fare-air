package com.flyadeal.app.api

import com.flyadeal.contract.api.ApiRoutes
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * API client for the flyadeal backend.
 * Uses Ktor client for cross-platform HTTP requests.
 */
class FlyadealApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    /**
     * Fetches the route map from the backend.
     * @return RouteMapResponse with available routes
     */
    suspend fun getRoutes(): ApiResult<RouteMapDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Config.ROUTES}").body()
        }
    }

    /**
     * Fetches all stations from the backend.
     * @return List of StationDto
     */
    suspend fun getStations(): ApiResult<List<StationDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Config.STATIONS}").body()
        }
    }

    /**
     * Searches for flights based on the given criteria.
     * @param request Search parameters
     * @return FlightSearchResponseDto with available flights
     */
    suspend fun searchFlights(request: FlightSearchRequestDto): ApiResult<FlightSearchResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Search.FLIGHTS}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    /**
     * Creates a booking.
     * @param request Booking details
     * @return BookingConfirmationDto on success
     */
    suspend fun createBooking(request: BookingRequestDto): ApiResult<BookingConfirmationDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Booking.CREATE}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    /**
     * Retrieves a booking by PNR.
     * @param pnr The 6-character PNR code
     * @return BookingConfirmationDto or error if not found
     */
    suspend fun getBooking(pnr: String): ApiResult<BookingConfirmationDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Booking.byPnr(pnr)}").body()
        }
    }

    /**
     * Wraps API calls with error handling.
     */
    private suspend inline fun <reified T> safeApiCall(block: () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            ApiResult.Error(
                code = "HTTP_${e.response.status.value}",
                message = errorBody.ifEmpty { e.message }
            )
        } catch (e: ServerResponseException) {
            ApiResult.Error(
                code = "SERVER_ERROR",
                message = "Server error: ${e.response.status.value}"
            )
        } catch (e: Exception) {
            ApiResult.Error(
                code = "NETWORK_ERROR",
                message = e.message ?: "Unknown error"
            )
        }
    }

    companion object {
        /**
         * Creates a configured HttpClient for the API.
         */
        fun createHttpClient(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
            val clientConfig: HttpClientConfig<*>.() -> Unit = {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                    })
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 30_000
                }
                defaultRequest {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }
            }

            return if (engine != null) {
                HttpClient(engine, clientConfig)
            } else {
                HttpClient(clientConfig)
            }
        }
    }
}

/**
 * Sealed class for API results.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): Error? = this as? Error

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Error) -> Unit): ApiResult<T> {
        if (this is Error) action(this)
        return this
    }
}

// DTO classes for API communication - matches backend response format

@Serializable
data class RouteMapDto(
    val routes: Map<String, List<String>>
)

@Serializable
data class StationDto(
    val code: String,
    val name: String,
    val city: String,
    val country: String
)

@Serializable
data class FlightSearchRequestDto(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val passengers: PassengerCountsDto
)

@Serializable
data class PassengerCountsDto(
    val adults: Int,
    val children: Int,
    val infants: Int
)

@Serializable
data class FlightSearchResponseDto(
    val flights: List<FlightDto>,
    val searchId: String = ""
)

/**
 * Flight DTO matching backend SearchController.FlightDto
 */
@Serializable
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int = 0,
    val durationFormatted: String = "",
    val aircraft: String = "",
    val fareFamilies: List<FareFamilyDto> = emptyList()
) {
    /** Convenience property for display */
    val duration: String get() = durationFormatted.ifEmpty { "${durationMinutes}m" }

    /** Convert fareFamilies to simpler fares for UI display */
    val fares: List<FareDto> get() = fareFamilies.map { ff ->
        FareDto(
            fareFamily = ff.name,
            basePrice = ff.priceFormatted,
            totalPrice = ff.priceFormatted,
            currency = ff.currency,
            inclusions = buildInclusionsList(ff.inclusions)
        )
    }

    private fun buildInclusionsList(inclusions: FareInclusionsDto): List<String> {
        val list = mutableListOf<String>()
        list.add("Carry-on: ${inclusions.carryOnBag}")
        inclusions.checkedBag?.let { list.add("Checked bag: $it") }
        list.add("Seat: ${inclusions.seatSelection}")
        if (inclusions.priorityBoarding) list.add("Priority boarding")
        if (inclusions.loungeAccess) list.add("Lounge access")
        return list
    }
}

/**
 * Fare family DTO matching backend SearchController.FareFamilyDto
 */
@Serializable
data class FareFamilyDto(
    val code: String,
    val name: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val inclusions: FareInclusionsDto
)

/**
 * Fare inclusions DTO matching backend SearchController.FareInclusionsDto
 */
@Serializable
data class FareInclusionsDto(
    val carryOnBag: String,
    val checkedBag: String? = null,
    val seatSelection: String,
    val changePolicy: String,
    val cancellationPolicy: String,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
)

/**
 * Simplified fare DTO for UI display.
 */
@Serializable
data class FareDto(
    val fareFamily: String,
    val basePrice: String = "0",
    val totalPrice: String = "0",
    val currency: String = "SAR",
    val inclusions: List<String> = emptyList()
)

@Serializable
data class BookingRequestDto(
    val searchId: String = "",
    val flightNumber: String,
    val fareFamily: String,
    val passengers: List<PassengerDto>,
    val ancillaries: List<AncillaryDto> = emptyList(),
    val contactEmail: String,
    val contactPhone: String = "",
    val payment: PaymentDto
)

@Serializable
data class PassengerDto(
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val documentExpiry: String = ""
)

@Serializable
data class AncillaryDto(
    val type: String,
    val passengerIndex: Int,
    val priceMinor: Long = 0,
    val currency: String = "SAR"
)

@Serializable
data class PaymentDto(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmountMinor: Long,
    val currency: String
)

@Serializable
data class BookingConfirmationDto(
    val pnr: String,
    val bookingReference: String = "",
    val status: String = "CONFIRMED",
    val totalPaidMinor: Long = 0,
    val totalPaidFormatted: String = "0",
    val currency: String = "SAR",
    val createdAt: String = ""
) {
    /** Convenience property for display */
    val totalPrice: String get() = totalPaidFormatted.ifEmpty { (totalPaidMinor / 100.0).toString() }
}
