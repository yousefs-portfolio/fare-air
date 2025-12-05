package com.fairair.app.admin.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * API client for Admin backend endpoints.
 */
class AdminApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private fun HttpRequestBuilder.addAuthHeader() {
        authToken?.let { header("X-Admin-Token", it) }
    }

    // ============================================================================
    // AUTHENTICATION
    // ============================================================================

    suspend fun login(email: String, password: String): AdminApiResult<AdminLoginResponse> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(email, password))
            }.body()
        }
    }

    suspend fun changePassword(adminId: String, oldPassword: String, newPassword: String): AdminApiResult<Map<String, Any>> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/auth/change-password") {
                addAuthHeader()
                header("X-Admin-Id", adminId)
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(oldPassword, newPassword))
            }.body()
        }
    }

    // ============================================================================
    // ADMIN USER MANAGEMENT
    // ============================================================================

    suspend fun getAllAdmins(): AdminApiResult<List<AdminDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/users") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getAdmin(id: String): AdminApiResult<AdminDto> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/users/$id") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun createAdmin(request: CreateAdminRequest): AdminApiResult<AdminDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/users") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun deactivateAdmin(id: String): AdminApiResult<Unit> {
        return safeAdminApiCall {
            httpClient.delete("$baseUrl/api/admin/users/$id") {
                addAuthHeader()
            }
            Unit
        }
    }

    // ============================================================================
    // STATIC PAGES
    // ============================================================================

    suspend fun getAllPages(): AdminApiResult<List<StaticPageDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/pages") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getPage(id: String): AdminApiResult<StaticPageDto> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/pages/$id") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun createPage(request: CreatePageRequest): AdminApiResult<StaticPageDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/pages") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun updatePage(id: String, request: UpdatePageRequest): AdminApiResult<StaticPageDto> {
        return safeAdminApiCall {
            httpClient.put("$baseUrl/api/admin/content/pages/$id") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun publishPage(id: String): AdminApiResult<StaticPageDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/pages/$id/publish") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun unpublishPage(id: String): AdminApiResult<StaticPageDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/pages/$id/unpublish") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun deletePage(id: String): AdminApiResult<Unit> {
        return safeAdminApiCall {
            httpClient.delete("$baseUrl/api/admin/content/pages/$id") {
                addAuthHeader()
            }
            Unit
        }
    }

    // ============================================================================
    // LEGAL DOCUMENTS
    // ============================================================================

    suspend fun getAllLegalDocuments(): AdminApiResult<List<LegalDocumentDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/legal") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getLegalDocumentsByType(type: String): AdminApiResult<List<LegalDocumentDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/legal/type/$type") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun createLegalDocument(request: CreateLegalDocumentRequest): AdminApiResult<LegalDocumentDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/legal") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun updateLegalDocument(id: String, request: UpdateLegalDocumentRequest): AdminApiResult<LegalDocumentDto> {
        return safeAdminApiCall {
            httpClient.put("$baseUrl/api/admin/content/legal/$id") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun publishLegalDocument(id: String): AdminApiResult<LegalDocumentDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/legal/$id/publish") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun setCurrentLegalVersion(id: String): AdminApiResult<LegalDocumentDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/legal/$id/set-current") {
                addAuthHeader()
            }.body()
        }
    }

    // ============================================================================
    // PROMOTIONS
    // ============================================================================

    suspend fun getAllPromotions(): AdminApiResult<List<PromotionDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/promotions") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getActivePromotions(): AdminApiResult<List<PromotionDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/promotions/active") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun createPromotion(request: CreatePromotionRequest): AdminApiResult<PromotionDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/promotions") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun updatePromotion(id: String, request: UpdatePromotionRequest): AdminApiResult<PromotionDto> {
        return safeAdminApiCall {
            httpClient.put("$baseUrl/api/admin/promotions/$id") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun deactivatePromotion(id: String): AdminApiResult<Unit> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/promotions/$id/deactivate") {
                addAuthHeader()
            }
            Unit
        }
    }

    // ============================================================================
    // DESTINATIONS
    // ============================================================================

    suspend fun getAllDestinations(): AdminApiResult<List<DestinationContentDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/destinations") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getDestination(id: String): AdminApiResult<DestinationContentDto> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/content/destinations/$id") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun createDestination(request: CreateDestinationRequest): AdminApiResult<DestinationContentDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/content/destinations") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun updateDestination(id: String, request: UpdateDestinationRequest): AdminApiResult<DestinationContentDto> {
        return safeAdminApiCall {
            httpClient.put("$baseUrl/api/admin/content/destinations/$id") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun deleteDestination(id: String): AdminApiResult<Unit> {
        return safeAdminApiCall {
            httpClient.delete("$baseUrl/api/admin/content/destinations/$id") {
                addAuthHeader()
            }
            Unit
        }
    }

    // ============================================================================
    // B2B - AGENCIES
    // ============================================================================

    suspend fun getAllAgencies(): AdminApiResult<List<AgencyDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/agencies") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getPendingAgencies(): AdminApiResult<List<AgencyDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/agencies/pending") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun approveAgency(id: String, request: ApproveAgencyRequest): AdminApiResult<AgencyDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/agencies/$id/approve") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun suspendAgency(id: String, notes: String?): AdminApiResult<AgencyDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/agencies/$id/suspend") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("notes" to notes))
            }.body()
        }
    }

    suspend fun rejectAgency(id: String, notes: String?): AdminApiResult<AgencyDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/agencies/$id/reject") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("notes" to notes))
            }.body()
        }
    }

    // ============================================================================
    // B2B - GROUP BOOKINGS
    // ============================================================================

    suspend fun getAllGroupBookings(): AdminApiResult<List<GroupBookingRequestDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/group-bookings") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getPendingGroupBookings(): AdminApiResult<List<GroupBookingRequestDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/group-bookings/pending") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun assignGroupBooking(id: String, adminId: String): AdminApiResult<GroupBookingRequestDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/group-bookings/$id/assign") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("assignedTo" to adminId))
            }.body()
        }
    }

    suspend fun submitGroupBookingQuote(id: String, request: SubmitQuoteRequest): AdminApiResult<GroupBookingRequestDto> {
        return safeAdminApiCall {
            httpClient.post("$baseUrl/api/admin/group-bookings/$id/quote") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    // ============================================================================
    // B2B - CHARTER REQUESTS
    // ============================================================================

    suspend fun getAllCharterRequests(): AdminApiResult<List<CharterRequestDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/charter") {
                addAuthHeader()
            }.body()
        }
    }

    suspend fun getPendingCharterRequests(): AdminApiResult<List<CharterRequestDto>> {
        return safeAdminApiCall {
            httpClient.get("$baseUrl/api/admin/charter/pending") {
                addAuthHeader()
            }.body()
        }
    }

    // ============================================================================
    // HELPER
    // ============================================================================

    private suspend inline fun <reified T> safeAdminApiCall(
        crossinline block: suspend () -> T
    ): AdminApiResult<T> {
        return try {
            AdminApiResult.Success(block())
        } catch (e: Exception) {
            AdminApiResult.Error(e.message ?: "Unknown error", e)
        }
    }
}

/**
 * Result wrapper for admin API calls.
 */
sealed class AdminApiResult<out T> {
    data class Success<T>(val data: T) : AdminApiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : AdminApiResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: Exception(message)
    }
}
