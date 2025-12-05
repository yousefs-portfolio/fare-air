package com.fairair.service

import com.fairair.entity.*
import com.fairair.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

// ============================================================================
// ADMIN AUTHENTICATION SERVICE
// ============================================================================

@Service
class AdminAuthService(
    private val adminUserRepository: AdminUserRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
    private val passwordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) {
    suspend fun authenticate(email: String, password: String): AdminUserEntity? {
        val admin = adminUserRepository.findByEmailIgnoreCase(email) ?: return null
        if (!admin.isActive) return null
        if (!passwordEncoder.matches(password, admin.passwordHash)) return null
        
        // Update last login
        val updated = admin.copy(lastLoginAt = Instant.now())
        return adminUserRepository.save(updated)
    }
    
    suspend fun createAdmin(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: AdminRole,
        department: String? = null
    ): AdminUserEntity {
        val existing = adminUserRepository.findByEmailIgnoreCase(email)
        if (existing != null) {
            throw IllegalArgumentException("Admin with email $email already exists")
        }
        
        val admin = AdminUserEntity(
            id = UUID.randomUUID().toString(),
            email = email.lowercase(),
            passwordHash = passwordEncoder.encode(password),
            firstName = firstName,
            lastName = lastName,
            role = role.name,
            department = department,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // Use insert for new entities with pre-generated IDs
        return r2dbcEntityTemplate.insert(admin).awaitSingle()
    }
    
    suspend fun getAdminById(id: String): AdminUserEntity? {
        return adminUserRepository.findById(id)
    }
    
    suspend fun updateAdmin(admin: AdminUserEntity): AdminUserEntity {
        return adminUserRepository.save(admin.copy(updatedAt = Instant.now()))
    }
    
    suspend fun deactivateAdmin(id: String): Boolean {
        val admin = adminUserRepository.findById(id) ?: return false
        adminUserRepository.save(admin.copy(isActive = false, updatedAt = Instant.now()))
        return true
    }
    
    fun getAllAdmins(): Flow<AdminUserEntity> {
        return adminUserRepository.findByIsActiveTrue()
    }
    
    fun getAdminsByRole(role: AdminRole): Flow<AdminUserEntity> {
        return adminUserRepository.findByRole(role.name)
    }
    
    suspend fun changePassword(adminId: String, oldPassword: String, newPassword: String): Boolean {
        val admin = adminUserRepository.findById(adminId) ?: return false
        if (!passwordEncoder.matches(oldPassword, admin.passwordHash)) return false
        
        adminUserRepository.save(admin.copy(
            passwordHash = passwordEncoder.encode(newPassword),
            updatedAt = Instant.now()
        ))
        return true
    }
}

// ============================================================================
// CONTENT MANAGEMENT SERVICE
// ============================================================================

@Service
class ContentManagementService(
    private val staticPageRepository: StaticPageRepository,
    private val legalDocumentRepository: LegalDocumentRepository,
    private val destinationContentRepository: DestinationContentRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    // ----------------------
    // STATIC PAGES
    // ----------------------
    
    suspend fun getPageBySlug(slug: String): StaticPageEntity? {
        return staticPageRepository.findBySlug(slug)
    }
    
    suspend fun getPublishedPageBySlug(slug: String): StaticPageEntity? {
        val page = staticPageRepository.findBySlug(slug)
        return if (page?.isPublished == true) page else null
    }
    
    fun getAllPages(): Flow<StaticPageEntity> {
        return staticPageRepository.findAllByOrderByUpdatedAtDesc()
    }
    
    fun getPublishedPages(): Flow<StaticPageEntity> {
        return staticPageRepository.findByIsPublishedTrue()
    }
    
    suspend fun createPage(
        slug: String,
        title: String,
        content: String,
        createdBy: String,
        titleAr: String? = null,
        contentAr: String? = null,
        metaDescription: String? = null,
        metaDescriptionAr: String? = null
    ): StaticPageEntity {
        val existing = staticPageRepository.findBySlug(slug)
        if (existing != null) {
            throw IllegalArgumentException("Page with slug '$slug' already exists")
        }
        
        val page = StaticPageEntity(
            id = UUID.randomUUID().toString(),
            slug = slug,
            title = title,
            titleAr = titleAr,
            content = content,
            contentAr = contentAr,
            metaDescription = metaDescription,
            metaDescriptionAr = metaDescriptionAr,
            isPublished = false,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(page).awaitSingle()
    }
    
    suspend fun updatePage(
        id: String,
        title: String? = null,
        content: String? = null,
        titleAr: String? = null,
        contentAr: String? = null,
        metaDescription: String? = null,
        metaDescriptionAr: String? = null,
        updatedBy: String
    ): StaticPageEntity? {
        val page = staticPageRepository.findById(id) ?: return null
        
        val updated = page.copy(
            title = title ?: page.title,
            content = content ?: page.content,
            titleAr = titleAr ?: page.titleAr,
            contentAr = contentAr ?: page.contentAr,
            metaDescription = metaDescription ?: page.metaDescription,
            metaDescriptionAr = metaDescriptionAr ?: page.metaDescriptionAr,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        
        return staticPageRepository.save(updated)
    }
    
    suspend fun publishPage(id: String, updatedBy: String): StaticPageEntity? {
        val page = staticPageRepository.findById(id) ?: return null
        val updated = page.copy(
            isPublished = true,
            publishedAt = Instant.now(),
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        return staticPageRepository.save(updated)
    }
    
    suspend fun unpublishPage(id: String, updatedBy: String): StaticPageEntity? {
        val page = staticPageRepository.findById(id) ?: return null
        val updated = page.copy(
            isPublished = false,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        return staticPageRepository.save(updated)
    }
    
    suspend fun deletePage(id: String): Boolean {
        val page = staticPageRepository.findById(id) ?: return false
        staticPageRepository.delete(page)
        return true
    }
    
    // ----------------------
    // LEGAL DOCUMENTS
    // ----------------------
    
    suspend fun getCurrentLegalDocument(type: LegalDocumentType): LegalDocumentEntity? {
        return legalDocumentRepository.findByTypeAndIsCurrent(type.name, true)
    }
    
    fun getLegalDocumentVersions(type: LegalDocumentType): Flow<LegalDocumentEntity> {
        return legalDocumentRepository.findByType(type.name)
    }
    
    fun getAllCurrentLegalDocuments(): Flow<LegalDocumentEntity> {
        return legalDocumentRepository.findByIsCurrentTrue()
    }
    
    suspend fun createLegalDocument(
        type: LegalDocumentType,
        version: String,
        title: String,
        content: String,
        effectiveDate: java.time.LocalDate,
        createdBy: String,
        titleAr: String? = null,
        contentAr: String? = null,
        makeCurrent: Boolean = false
    ): LegalDocumentEntity {
        // If making current, un-mark the previous current version
        if (makeCurrent) {
            val current = legalDocumentRepository.findByTypeAndIsCurrent(type.name, true)
            if (current != null) {
                legalDocumentRepository.save(current.copy(isCurrent = false))
            }
        }
        
        val document = LegalDocumentEntity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            version = version,
            title = title,
            titleAr = titleAr,
            content = content,
            contentAr = contentAr,
            effectiveDate = effectiveDate,
            isCurrent = makeCurrent,
            createdBy = createdBy,
            createdAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(document).awaitSingle()
    }
    
    suspend fun setCurrentVersion(documentId: String): LegalDocumentEntity? {
        val document = legalDocumentRepository.findById(documentId) ?: return null
        
        // Un-mark current version for this type
        val current = legalDocumentRepository.findByTypeAndIsCurrent(document.type, true)
        if (current != null && current.id != documentId) {
            legalDocumentRepository.save(current.copy(isCurrent = false))
        }
        
        return legalDocumentRepository.save(document.copy(isCurrent = true))
    }
    
    // ----------------------
    // DESTINATION CONTENT
    // ----------------------
    
    suspend fun getDestination(airportCode: String): DestinationContentEntity? {
        return destinationContentRepository.findByAirportCode(airportCode)
    }
    
    fun getFeaturedDestinations(): Flow<DestinationContentEntity> {
        return destinationContentRepository.findByIsFeaturedTrue()
    }
    
    fun getPublishedDestinations(): Flow<DestinationContentEntity> {
        return destinationContentRepository.findByIsPublishedTrue()
    }
    
    suspend fun createDestination(
        airportCode: String,
        cityName: String,
        country: String,
        createdBy: String,
        cityNameAr: String? = null,
        countryAr: String? = null,
        description: String? = null,
        descriptionAr: String? = null,
        highlights: String? = null,
        highlightsAr: String? = null,
        imageUrl: String? = null,
        galleryUrls: String? = null,
        lowestFare: java.math.BigDecimal? = null,
        isFeatured: Boolean = false
    ): DestinationContentEntity {
        val existing = destinationContentRepository.findByAirportCode(airportCode)
        if (existing != null) {
            throw IllegalArgumentException("Destination for $airportCode already exists")
        }
        
        val destination = DestinationContentEntity(
            id = UUID.randomUUID().toString(),
            airportCode = airportCode,
            cityName = cityName,
            cityNameAr = cityNameAr,
            country = country,
            countryAr = countryAr,
            description = description,
            descriptionAr = descriptionAr,
            highlights = highlights,
            highlightsAr = highlightsAr,
            imageUrl = imageUrl,
            galleryUrls = galleryUrls,
            lowestFare = lowestFare,
            isFeatured = isFeatured,
            isPublished = true,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(destination).awaitSingle()
    }
    
    suspend fun updateDestination(
        id: String,
        updatedBy: String,
        cityName: String? = null,
        cityNameAr: String? = null,
        countryAr: String? = null,
        description: String? = null,
        descriptionAr: String? = null,
        highlights: String? = null,
        highlightsAr: String? = null,
        imageUrl: String? = null,
        galleryUrls: String? = null,
        lowestFare: java.math.BigDecimal? = null,
        isFeatured: Boolean? = null,
        isPublished: Boolean? = null
    ): DestinationContentEntity? {
        val destination = destinationContentRepository.findById(id) ?: return null
        
        val updated = destination.copy(
            cityName = cityName ?: destination.cityName,
            cityNameAr = cityNameAr ?: destination.cityNameAr,
            countryAr = countryAr ?: destination.countryAr,
            description = description ?: destination.description,
            descriptionAr = descriptionAr ?: destination.descriptionAr,
            highlights = highlights ?: destination.highlights,
            highlightsAr = highlightsAr ?: destination.highlightsAr,
            imageUrl = imageUrl ?: destination.imageUrl,
            galleryUrls = galleryUrls ?: destination.galleryUrls,
            lowestFare = lowestFare ?: destination.lowestFare,
            isFeatured = isFeatured ?: destination.isFeatured,
            isPublished = isPublished ?: destination.isPublished,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        
        return destinationContentRepository.save(updated)
    }
    
    suspend fun deleteDestination(id: String): Boolean {
        val destination = destinationContentRepository.findById(id) ?: return false
        destinationContentRepository.delete(destination)
        return true
    }
}
