package com.flyadeal.exception

import org.springframework.http.HttpStatus

/**
 * Base exception for all application-specific errors.
 * Provides structured error information for consistent API responses.
 */
open class FlyadealException(
    override val message: String,
    val errorCode: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when a requested resource is not found.
 */
class NotFoundException(
    message: String,
    details: Map<String, Any>? = null
) : FlyadealException(
    message = message,
    errorCode = "NOT_FOUND",
    httpStatus = HttpStatus.NOT_FOUND,
    details = details
)

/**
 * Exception thrown when request validation fails.
 */
class ValidationException(
    message: String,
    details: Map<String, Any>? = null
) : FlyadealException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST,
    details = details
)

/**
 * Exception thrown when booking creation fails.
 */
class BookingException(
    message: String,
    details: Map<String, Any>? = null,
    cause: Throwable? = null
) : FlyadealException(
    message = message,
    errorCode = "BOOKING_ERROR",
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
    details = details,
    cause = cause
)

/**
 * Exception thrown when external service (Navitaire) fails.
 */
class ExternalServiceException(
    message: String,
    serviceName: String,
    cause: Throwable? = null
) : FlyadealException(
    message = message,
    errorCode = "EXTERNAL_SERVICE_ERROR",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    details = mapOf("service" to serviceName),
    cause = cause
)

/**
 * Exception thrown when a search session has expired.
 */
class SearchExpiredException(
    val searchId: String
) : FlyadealException(
    message = "Search session has expired. Please search again.",
    errorCode = "SEARCH_EXPIRED",
    httpStatus = HttpStatus.GONE,
    details = mapOf("searchId" to searchId)
)

/**
 * Exception thrown when a flight is not found.
 */
class FlightNotFoundException(
    val flightNumber: String
) : FlyadealException(
    message = "Selected flight is no longer available.",
    errorCode = "FLIGHT_NOT_FOUND",
    httpStatus = HttpStatus.NOT_FOUND,
    details = mapOf("flightNumber" to flightNumber)
)

/**
 * Exception thrown when a fare is not found.
 */
class FareNotFoundException(
    val fareFamily: String
) : FlyadealException(
    message = "Selected fare is no longer available.",
    errorCode = "FARE_NOT_FOUND",
    httpStatus = HttpStatus.NOT_FOUND,
    details = mapOf("fareFamily" to fareFamily)
)

/**
 * Exception thrown when passenger validation fails.
 */
class PassengerValidationException(
    override val message: String
) : FlyadealException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * Exception thrown when ancillary validation fails.
 */
class AncillaryValidationException(
    override val message: String
) : FlyadealException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * Exception thrown when payment validation fails.
 */
class PaymentValidationException(
    override val message: String
) : FlyadealException(
    message = message,
    errorCode = "PAYMENT_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST
)
