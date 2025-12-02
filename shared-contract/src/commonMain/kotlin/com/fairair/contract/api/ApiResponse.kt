package com.fairair.contract.api

import kotlinx.serialization.Serializable

/**
 * Standard API error response structure.
 * Used for all error responses from the backend.
 */
@Serializable
data class ApiError(
    val code: ErrorCode,
    val message: String,
    val details: Map<String, String>? = null,
    val timestamp: String
) {
    companion object {
        /**
         * Creates an ApiError for a validation failure.
         * @param field The field that failed validation
         * @param message The error message
         * @param timestamp ISO-8601 timestamp
         * @return An ApiError instance
         */
        fun validation(field: String, message: String, timestamp: String): ApiError =
            ApiError(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Validation failed for field '$field'",
                details = mapOf("field" to field, "error" to message),
                timestamp = timestamp
            )

        /**
         * Creates an ApiError for a not found scenario.
         * @param resource The resource type (e.g., "booking", "flight")
         * @param id The identifier that was not found
         * @param timestamp ISO-8601 timestamp
         * @return An ApiError instance
         */
        fun notFound(resource: String, id: String, timestamp: String): ApiError =
            ApiError(
                code = ErrorCode.NOT_FOUND,
                message = "$resource not found: $id",
                details = mapOf("resource" to resource, "id" to id),
                timestamp = timestamp
            )
    }
}

/**
 * Error codes for API responses.
 * Maps to HTTP status codes and provides machine-readable error identification.
 */
@Serializable
enum class ErrorCode(val httpStatus: Int, val description: String) {
    /**
     * Request payload failed validation.
     * HTTP 400 Bad Request
     */
    VALIDATION_ERROR(400, "Request validation failed"),

    /**
     * Missing or invalid authentication.
     * HTTP 401 Unauthorized
     */
    UNAUTHORIZED(401, "Authentication required"),

    /**
     * User does not have permission for this action.
     * HTTP 403 Forbidden
     */
    FORBIDDEN(403, "Access denied"),

    /**
     * Requested resource was not found.
     * HTTP 404 Not Found
     */
    NOT_FOUND(404, "Resource not found"),

    /**
     * Invalid route (origin/destination combination not available).
     * HTTP 400 Bad Request
     */
    INVALID_ROUTE(400, "Route not available"),

    /**
     * No flights available for the search criteria.
     * HTTP 200 (success, but empty results)
     */
    NO_FLIGHTS(200, "No flights available"),

    /**
     * Search session expired, user must search again.
     * HTTP 410 Gone
     */
    SEARCH_EXPIRED(410, "Search session expired"),

    /**
     * Flight/fare no longer available (sold out).
     * HTTP 409 Conflict
     */
    FARE_UNAVAILABLE(409, "Selected fare no longer available"),

    /**
     * Payment processing failed.
     * HTTP 402 Payment Required
     */
    PAYMENT_FAILED(402, "Payment processing failed"),

    /**
     * Booking creation failed.
     * HTTP 500 Internal Server Error
     */
    BOOKING_FAILED(500, "Unable to complete booking"),

    /**
     * Unexpected server error.
     * HTTP 500 Internal Server Error
     */
    INTERNAL_ERROR(500, "Internal server error"),

    /**
     * Service temporarily unavailable (maintenance, etc.).
     * HTTP 503 Service Unavailable
     */
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable")
}

/**
 * Generic wrapper for API responses that may succeed or fail.
 * Useful for frontend state management.
 */
@Serializable
sealed class ApiResult<out T> {
    /**
     * Successful response with data.
     */
    @Serializable
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * Error response.
     */
    @Serializable
    data class Error(val error: ApiError) : ApiResult<Nothing>()

    /**
     * Returns true if this is a successful result.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an error result.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns the data if successful, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Returns the error if failed, null otherwise.
     */
    fun errorOrNull(): ApiError? = when (this) {
        is Success -> null
        is Error -> error
    }

    /**
     * Maps the success value to a new type.
     */
    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
    }

    /**
     * Executes the given block if this is a success.
     */
    inline fun onSuccess(block: (T) -> Unit): ApiResult<T> {
        if (this is Success) block(data)
        return this
    }

    /**
     * Executes the given block if this is an error.
     */
    inline fun onError(block: (ApiError) -> Unit): ApiResult<T> {
        if (this is Error) block(error)
        return this
    }
}
