package com.fairair.repository

import com.fairair.entity.*
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

// ============================================================================
// ADMIN REPOSITORIES
// ============================================================================

@Repository
interface AdminUserRepository : CoroutineCrudRepository<AdminUserEntity, String> {
    suspend fun findByEmailIgnoreCase(email: String): AdminUserEntity?
    fun findByRole(role: String): Flow<AdminUserEntity>
    fun findByIsActiveTrue(): Flow<AdminUserEntity>
}

@Repository
interface StaticPageRepository : CoroutineCrudRepository<StaticPageEntity, String> {
    suspend fun findBySlug(slug: String): StaticPageEntity?
    fun findByIsPublishedTrue(): Flow<StaticPageEntity>
    fun findAllByOrderByUpdatedAtDesc(): Flow<StaticPageEntity>
}

@Repository
interface LegalDocumentRepository : CoroutineCrudRepository<LegalDocumentEntity, String> {
    suspend fun findByTypeAndIsCurrent(type: String, isCurrent: Boolean): LegalDocumentEntity?
    fun findByType(type: String): Flow<LegalDocumentEntity>
    fun findByIsCurrentTrue(): Flow<LegalDocumentEntity>
}

@Repository
interface PromotionRepository : CoroutineCrudRepository<PromotionEntity, String> {
    suspend fun findByCode(code: String): PromotionEntity?
    fun findByIsActiveTrue(): Flow<PromotionEntity>
    fun findAllByOrderByStartDateDesc(): Flow<PromotionEntity>
}

@Repository
interface DestinationContentRepository : CoroutineCrudRepository<DestinationContentEntity, String> {
    suspend fun findByAirportCode(airportCode: String): DestinationContentEntity?
    fun findByIsFeaturedTrue(): Flow<DestinationContentEntity>
    fun findByIsPublishedTrue(): Flow<DestinationContentEntity>
}

// ============================================================================
// B2B REPOSITORIES
// ============================================================================

@Repository
interface AgencyRepository : CoroutineCrudRepository<AgencyEntity, String> {
    suspend fun findByAgencyCode(agencyCode: String): AgencyEntity?
    suspend fun findByContactEmail(contactEmail: String): AgencyEntity?
    fun findByStatus(status: String): Flow<AgencyEntity>
    fun findByType(type: String): Flow<AgencyEntity>
    fun findAllByOrderByCreatedAtDesc(): Flow<AgencyEntity>
}

@Repository
interface AgencyUserRepository : CoroutineCrudRepository<AgencyUserEntity, String> {
    suspend fun findByEmailIgnoreCase(email: String): AgencyUserEntity?
    fun findByAgencyId(agencyId: String): Flow<AgencyUserEntity>
    fun findByAgencyIdAndIsActiveTrue(agencyId: String): Flow<AgencyUserEntity>
}

@Repository
interface GroupBookingRequestRepository : CoroutineCrudRepository<GroupBookingRequestEntity, String> {
    suspend fun findByRequestNumber(requestNumber: String): GroupBookingRequestEntity?
    fun findByAgencyId(agencyId: String): Flow<GroupBookingRequestEntity>
    fun findByStatus(status: String): Flow<GroupBookingRequestEntity>
    fun findByAssignedTo(assignedTo: String): Flow<GroupBookingRequestEntity>
    fun findAllByOrderByCreatedAtDesc(): Flow<GroupBookingRequestEntity>
}

@Repository
interface CharterRequestRepository : CoroutineCrudRepository<CharterRequestEntity, String> {
    suspend fun findByRequestNumber(requestNumber: String): CharterRequestEntity?
    fun findByAgencyId(agencyId: String): Flow<CharterRequestEntity>
    fun findByStatus(status: String): Flow<CharterRequestEntity>
    fun findByCharterType(charterType: String): Flow<CharterRequestEntity>
    fun findAllByOrderByCreatedAtDesc(): Flow<CharterRequestEntity>
}

@Repository
interface AgencyBookingRepository : CoroutineCrudRepository<AgencyBookingEntity, String> {
    fun findByAgencyId(agencyId: String): Flow<AgencyBookingEntity>
    fun findByAgentUserId(agentUserId: String): Flow<AgencyBookingEntity>
    suspend fun findByBookingPnr(bookingPnr: String): AgencyBookingEntity?
}
