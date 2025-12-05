package com.fairair.controller

import com.fairair.entity.*
import com.fairair.service.*
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

// ============================================================================
// ADMIN AUTHENTICATION CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/admin/auth")
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) {
    @PostMapping("/login")
    suspend fun login(@RequestBody request: AdminLoginRequest): ResponseEntity<AdminLoginResponse> {
        val admin = adminAuthService.authenticate(request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AdminLoginResponse(success = false, message = "Invalid credentials"))
        
        return ResponseEntity.ok(AdminLoginResponse(
            success = true,
            message = "Login successful",
            admin = AdminDto.from(admin),
            token = generateToken(admin) // TODO: Implement proper JWT
        ))
    }
    
    @PostMapping("/change-password")
    suspend fun changePassword(
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, Any>> {
        val success = adminAuthService.changePassword(adminId, request.oldPassword, request.newPassword)
        return if (success) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Password changed"))
        } else {
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "Invalid old password"))
        }
    }
    
    private fun generateToken(admin: AdminUserEntity): String {
        // TODO: Implement proper JWT token generation
        return "admin-token-${admin.id}"
    }
}

data class AdminLoginRequest(val email: String, val password: String)
data class AdminLoginResponse(
    val success: Boolean,
    val message: String,
    val admin: AdminDto? = null,
    val token: String? = null
)
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)
data class AdminDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val department: String?,
    val isActive: Boolean
) {
    companion object {
        fun from(entity: AdminUserEntity) = AdminDto(
            id = entity.id,
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            role = entity.role,
            department = entity.department,
            isActive = entity.isActive
        )
    }
}

// ============================================================================
// ADMIN USER MANAGEMENT CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val adminAuthService: AdminAuthService
) {
    @GetMapping
    suspend fun getAllAdmins(): ResponseEntity<List<AdminDto>> {
        val admins = adminAuthService.getAllAdmins().toList()
        return ResponseEntity.ok(admins.map { AdminDto.from(it) })
    }
    
    @GetMapping("/{id}")
    suspend fun getAdmin(@PathVariable id: String): ResponseEntity<AdminDto> {
        val admin = adminAuthService.getAdminById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AdminDto.from(admin))
    }
    
    @PostMapping
    suspend fun createAdmin(@RequestBody request: CreateAdminRequest): ResponseEntity<AdminDto> {
        return try {
            val admin = adminAuthService.createAdmin(
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                role = AdminRole.valueOf(request.role),
                department = request.department
            )
            ResponseEntity.status(HttpStatus.CREATED).body(AdminDto.from(admin))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @DeleteMapping("/{id}")
    suspend fun deactivateAdmin(@PathVariable id: String): ResponseEntity<Void> {
        return if (adminAuthService.deactivateAdmin(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class CreateAdminRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val department: String? = null
)

// ============================================================================
// CONTENT MANAGEMENT CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/admin/content")
class ContentManagementController(
    private val contentService: ContentManagementService
) {
    // ----------------------
    // STATIC PAGES
    // ----------------------
    
    @GetMapping("/pages")
    suspend fun getAllPages(): ResponseEntity<List<StaticPageDto>> {
        val pages = contentService.getAllPages().toList()
        return ResponseEntity.ok(pages.map { StaticPageDto.from(it) })
    }
    
    @GetMapping("/pages/{slug}")
    suspend fun getPage(@PathVariable slug: String): ResponseEntity<StaticPageDto> {
        val page = contentService.getPageBySlug(slug)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StaticPageDto.from(page))
    }
    
    @PostMapping("/pages")
    suspend fun createPage(
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: CreatePageRequest
    ): ResponseEntity<StaticPageDto> {
        return try {
            val page = contentService.createPage(
                slug = request.slug,
                title = request.title,
                content = request.content,
                createdBy = adminId,
                titleAr = request.titleAr,
                contentAr = request.contentAr,
                metaDescription = request.metaDescription,
                metaDescriptionAr = request.metaDescriptionAr
            )
            ResponseEntity.status(HttpStatus.CREATED).body(StaticPageDto.from(page))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/pages/{id}")
    suspend fun updatePage(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: UpdatePageRequest
    ): ResponseEntity<StaticPageDto> {
        val page = contentService.updatePage(
            id = id,
            updatedBy = adminId,
            title = request.title,
            content = request.content,
            titleAr = request.titleAr,
            contentAr = request.contentAr,
            metaDescription = request.metaDescription,
            metaDescriptionAr = request.metaDescriptionAr
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StaticPageDto.from(page))
    }
    
    @PostMapping("/pages/{id}/publish")
    suspend fun publishPage(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String
    ): ResponseEntity<StaticPageDto> {
        val page = contentService.publishPage(id, adminId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StaticPageDto.from(page))
    }
    
    @PostMapping("/pages/{id}/unpublish")
    suspend fun unpublishPage(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String
    ): ResponseEntity<StaticPageDto> {
        val page = contentService.unpublishPage(id, adminId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StaticPageDto.from(page))
    }
    
    @DeleteMapping("/pages/{id}")
    suspend fun deletePage(@PathVariable id: String): ResponseEntity<Void> {
        return if (contentService.deletePage(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    // ----------------------
    // LEGAL DOCUMENTS
    // ----------------------
    
    @GetMapping("/legal")
    suspend fun getAllLegalDocuments(): ResponseEntity<List<LegalDocumentDto>> {
        val documents = contentService.getAllCurrentLegalDocuments().toList()
        return ResponseEntity.ok(documents.map { LegalDocumentDto.from(it) })
    }
    
    @GetMapping("/legal/{type}")
    suspend fun getCurrentLegalDocument(
        @PathVariable type: String
    ): ResponseEntity<LegalDocumentDto> {
        val docType = try { LegalDocumentType.valueOf(type) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val document = contentService.getCurrentLegalDocument(docType)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(LegalDocumentDto.from(document))
    }
    
    @GetMapping("/legal/{type}/versions")
    suspend fun getLegalDocumentVersions(
        @PathVariable type: String
    ): ResponseEntity<List<LegalDocumentDto>> {
        val docType = try { LegalDocumentType.valueOf(type) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val documents = contentService.getLegalDocumentVersions(docType).toList()
        return ResponseEntity.ok(documents.map { LegalDocumentDto.from(it) })
    }
    
    @PostMapping("/legal")
    suspend fun createLegalDocument(
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: CreateLegalDocumentRequest
    ): ResponseEntity<LegalDocumentDto> {
        val docType = try { LegalDocumentType.valueOf(request.type) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        
        val document = contentService.createLegalDocument(
            type = docType,
            version = request.version,
            title = request.title,
            content = request.content,
            effectiveDate = request.effectiveDate,
            createdBy = adminId,
            titleAr = request.titleAr,
            contentAr = request.contentAr,
            makeCurrent = request.makeCurrent
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(LegalDocumentDto.from(document))
    }
    
    @PostMapping("/legal/{id}/set-current")
    suspend fun setCurrentVersion(@PathVariable id: String): ResponseEntity<LegalDocumentDto> {
        val document = contentService.setCurrentVersion(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(LegalDocumentDto.from(document))
    }
    
    // ----------------------
    // DESTINATIONS
    // ----------------------
    
    @GetMapping("/destinations")
    suspend fun getAllDestinations(): ResponseEntity<List<DestinationDto>> {
        val destinations = contentService.getPublishedDestinations().toList()
        return ResponseEntity.ok(destinations.map { DestinationDto.from(it) })
    }
    
    @GetMapping("/destinations/featured")
    suspend fun getFeaturedDestinations(): ResponseEntity<List<DestinationDto>> {
        val destinations = contentService.getFeaturedDestinations().toList()
        return ResponseEntity.ok(destinations.map { DestinationDto.from(it) })
    }
    
    @GetMapping("/destinations/{airportCode}")
    suspend fun getDestination(@PathVariable airportCode: String): ResponseEntity<DestinationDto> {
        val destination = contentService.getDestination(airportCode)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(DestinationDto.from(destination))
    }
    
    @PostMapping("/destinations")
    suspend fun createDestination(
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: CreateDestinationRequest
    ): ResponseEntity<DestinationDto> {
        return try {
            val destination = contentService.createDestination(
                airportCode = request.airportCode,
                cityName = request.cityName,
                country = request.country,
                createdBy = adminId,
                cityNameAr = request.cityNameAr,
                countryAr = request.countryAr,
                description = request.description,
                descriptionAr = request.descriptionAr,
                highlights = request.highlights,
                highlightsAr = request.highlightsAr,
                imageUrl = request.imageUrl,
                galleryUrls = request.galleryUrls,
                lowestFare = request.lowestFare,
                isFeatured = request.isFeatured
            )
            ResponseEntity.status(HttpStatus.CREATED).body(DestinationDto.from(destination))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/destinations/{id}")
    suspend fun updateDestination(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: UpdateDestinationRequest
    ): ResponseEntity<DestinationDto> {
        val destination = contentService.updateDestination(
            id = id,
            updatedBy = adminId,
            cityName = request.cityName,
            cityNameAr = request.cityNameAr,
            countryAr = request.countryAr,
            description = request.description,
            descriptionAr = request.descriptionAr,
            highlights = request.highlights,
            highlightsAr = request.highlightsAr,
            imageUrl = request.imageUrl,
            galleryUrls = request.galleryUrls,
            lowestFare = request.lowestFare,
            isFeatured = request.isFeatured,
            isPublished = request.isPublished
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(DestinationDto.from(destination))
    }
    
    @DeleteMapping("/destinations/{id}")
    suspend fun deleteDestination(@PathVariable id: String): ResponseEntity<Void> {
        return if (contentService.deleteDestination(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

// DTOs
data class CreatePageRequest(
    val slug: String,
    val title: String,
    val content: String,
    val titleAr: String? = null,
    val contentAr: String? = null,
    val metaDescription: String? = null,
    val metaDescriptionAr: String? = null
)

data class UpdatePageRequest(
    val title: String? = null,
    val content: String? = null,
    val titleAr: String? = null,
    val contentAr: String? = null,
    val metaDescription: String? = null,
    val metaDescriptionAr: String? = null
)

data class StaticPageDto(
    val id: String,
    val slug: String,
    val title: String,
    val titleAr: String?,
    val content: String,
    val contentAr: String?,
    val metaDescription: String?,
    val isPublished: Boolean,
    val publishedAt: Instant?,
    val updatedAt: Instant
) {
    companion object {
        fun from(entity: StaticPageEntity) = StaticPageDto(
            id = entity.id,
            slug = entity.slug,
            title = entity.title,
            titleAr = entity.titleAr,
            content = entity.content,
            contentAr = entity.contentAr,
            metaDescription = entity.metaDescription,
            isPublished = entity.isPublished,
            publishedAt = entity.publishedAt,
            updatedAt = entity.updatedAt
        )
    }
}

data class CreateLegalDocumentRequest(
    val type: String,
    val version: String,
    val title: String,
    val content: String,
    val effectiveDate: LocalDate,
    val titleAr: String? = null,
    val contentAr: String? = null,
    val makeCurrent: Boolean = false
)

data class LegalDocumentDto(
    val id: String,
    val type: String,
    val version: String,
    val title: String,
    val titleAr: String?,
    val content: String,
    val contentAr: String?,
    val effectiveDate: LocalDate,
    val isCurrent: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: LegalDocumentEntity) = LegalDocumentDto(
            id = entity.id,
            type = entity.type,
            version = entity.version,
            title = entity.title,
            titleAr = entity.titleAr,
            content = entity.content,
            contentAr = entity.contentAr,
            effectiveDate = entity.effectiveDate,
            isCurrent = entity.isCurrent,
            createdAt = entity.createdAt
        )
    }
}

data class CreateDestinationRequest(
    val airportCode: String,
    val cityName: String,
    val country: String,
    val cityNameAr: String? = null,
    val countryAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val highlights: String? = null,
    val highlightsAr: String? = null,
    val imageUrl: String? = null,
    val galleryUrls: String? = null,
    val lowestFare: java.math.BigDecimal? = null,
    val isFeatured: Boolean = false
)

data class UpdateDestinationRequest(
    val cityName: String? = null,
    val cityNameAr: String? = null,
    val countryAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val highlights: String? = null,
    val highlightsAr: String? = null,
    val imageUrl: String? = null,
    val galleryUrls: String? = null,
    val lowestFare: java.math.BigDecimal? = null,
    val isFeatured: Boolean? = null,
    val isPublished: Boolean? = null
)

data class DestinationDto(
    val id: String,
    val airportCode: String,
    val cityName: String,
    val cityNameAr: String?,
    val country: String,
    val countryAr: String?,
    val description: String?,
    val descriptionAr: String?,
    val highlights: String?,
    val imageUrl: String?,
    val lowestFare: java.math.BigDecimal?,
    val isFeatured: Boolean,
    val isPublished: Boolean
) {
    companion object {
        fun from(entity: DestinationContentEntity) = DestinationDto(
            id = entity.id,
            airportCode = entity.airportCode,
            cityName = entity.cityName,
            cityNameAr = entity.cityNameAr,
            country = entity.country,
            countryAr = entity.countryAr,
            description = entity.description,
            descriptionAr = entity.descriptionAr,
            highlights = entity.highlights,
            imageUrl = entity.imageUrl,
            lowestFare = entity.lowestFare,
            isFeatured = entity.isFeatured,
            isPublished = entity.isPublished
        )
    }
}

// ============================================================================
// PROMOTIONS CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/admin/promotions")
class PromotionsController(
    private val promotionService: PromotionService
) {
    @GetMapping
    suspend fun getAllPromotions(): ResponseEntity<List<PromotionDto>> {
        val promotions = promotionService.getAllPromotions().toList()
        return ResponseEntity.ok(promotions.map { PromotionDto.from(it) })
    }
    
    @GetMapping("/active")
    suspend fun getActivePromotions(): ResponseEntity<List<PromotionDto>> {
        val promotions = promotionService.getActivePromotions().toList()
        return ResponseEntity.ok(promotions.map { PromotionDto.from(it) })
    }
    
    @GetMapping("/validate/{code}")
    suspend fun validatePromoCode(
        @PathVariable code: String,
        @RequestParam amount: java.math.BigDecimal,
        @RequestParam(required = false) origin: String?,
        @RequestParam(required = false) destination: String?,
        @RequestParam(required = false) fareFamily: String?
    ): ResponseEntity<PromotionValidationResponse> {
        val result = promotionService.validateAndApplyPromotion(
            code = code,
            amount = amount,
            origin = origin,
            destination = destination,
            fareFamily = fareFamily
        )
        return ResponseEntity.ok(PromotionValidationResponse(
            isValid = result.isValid,
            message = result.message,
            discountAmount = result.discountAmount,
            promotion = result.promotion?.let { PromotionDto.from(it) }
        ))
    }
    
    @PostMapping
    suspend fun createPromotion(
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: CreatePromotionRequest
    ): ResponseEntity<PromotionDto> {
        return try {
            val promotion = promotionService.createPromotion(
                title = request.title,
                discountType = DiscountType.valueOf(request.discountType),
                discountValue = request.discountValue,
                startDate = request.startDate,
                endDate = request.endDate,
                createdBy = adminId,
                code = request.code,
                titleAr = request.titleAr,
                description = request.description,
                descriptionAr = request.descriptionAr,
                currency = request.currency ?: "SAR",
                minPurchaseAmount = request.minPurchaseAmount,
                maxDiscountAmount = request.maxDiscountAmount,
                originCode = request.originCode,
                destinationCode = request.destinationCode,
                fareFamily = request.fareFamily,
                imageUrl = request.imageUrl,
                maxUses = request.maxUses
            )
            ResponseEntity.status(HttpStatus.CREATED).body(PromotionDto.from(promotion))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/{id}")
    suspend fun updatePromotion(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: UpdatePromotionRequest
    ): ResponseEntity<PromotionDto> {
        val promotion = promotionService.updatePromotion(
            id = id,
            updatedBy = adminId,
            title = request.title,
            titleAr = request.titleAr,
            description = request.description,
            descriptionAr = request.descriptionAr,
            discountValue = request.discountValue,
            minPurchaseAmount = request.minPurchaseAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            startDate = request.startDate,
            endDate = request.endDate,
            maxUses = request.maxUses,
            isActive = request.isActive,
            imageUrl = request.imageUrl
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(PromotionDto.from(promotion))
    }
    
    @DeleteMapping("/{id}")
    suspend fun deactivatePromotion(@PathVariable id: String): ResponseEntity<Void> {
        return if (promotionService.deactivatePromotion(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class CreatePromotionRequest(
    val code: String? = null,
    val title: String,
    val titleAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val discountType: String,
    val discountValue: java.math.BigDecimal,
    val currency: String? = null,
    val minPurchaseAmount: java.math.BigDecimal? = null,
    val maxDiscountAmount: java.math.BigDecimal? = null,
    val originCode: String? = null,
    val destinationCode: String? = null,
    val fareFamily: String? = null,
    val imageUrl: String? = null,
    val startDate: Instant,
    val endDate: Instant,
    val maxUses: Int? = null
)

data class UpdatePromotionRequest(
    val title: String? = null,
    val titleAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val discountValue: java.math.BigDecimal? = null,
    val minPurchaseAmount: java.math.BigDecimal? = null,
    val maxDiscountAmount: java.math.BigDecimal? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val maxUses: Int? = null,
    val isActive: Boolean? = null,
    val imageUrl: String? = null
)

data class PromotionDto(
    val id: String,
    val code: String?,
    val title: String,
    val titleAr: String?,
    val description: String?,
    val discountType: String,
    val discountValue: java.math.BigDecimal,
    val currency: String,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean,
    val currentUses: Int,
    val maxUses: Int?
) {
    companion object {
        fun from(entity: PromotionEntity) = PromotionDto(
            id = entity.id,
            code = entity.code,
            title = entity.title,
            titleAr = entity.titleAr,
            description = entity.description,
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            currency = entity.currency,
            startDate = entity.startDate,
            endDate = entity.endDate,
            isActive = entity.isActive,
            currentUses = entity.currentUses,
            maxUses = entity.maxUses
        )
    }
}

data class PromotionValidationResponse(
    val isValid: Boolean,
    val message: String,
    val discountAmount: java.math.BigDecimal,
    val promotion: PromotionDto? = null
)

// ============================================================================
// PUBLIC CONTENT CONTROLLER (for frontend)
// ============================================================================

@RestController
@RequestMapping("/api/content")
class PublicContentController(
    private val contentService: ContentManagementService
) {
    @GetMapping("/pages/{slug}")
    suspend fun getPage(@PathVariable slug: String): ResponseEntity<StaticPageDto> {
        val page = contentService.getPublishedPageBySlug(slug)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StaticPageDto.from(page))
    }
    
    @GetMapping("/legal/{type}")
    suspend fun getLegalDocument(@PathVariable type: String): ResponseEntity<LegalDocumentDto> {
        val docType = try { LegalDocumentType.valueOf(type) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val document = contentService.getCurrentLegalDocument(docType)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(LegalDocumentDto.from(document))
    }
    
    @GetMapping("/destinations")
    suspend fun getDestinations(): ResponseEntity<List<DestinationDto>> {
        val destinations = contentService.getPublishedDestinations().toList()
        return ResponseEntity.ok(destinations.map { DestinationDto.from(it) })
    }
    
    @GetMapping("/destinations/featured")
    suspend fun getFeaturedDestinations(): ResponseEntity<List<DestinationDto>> {
        val destinations = contentService.getFeaturedDestinations().toList()
        return ResponseEntity.ok(destinations.map { DestinationDto.from(it) })
    }
    
    @GetMapping("/destinations/{airportCode}")
    suspend fun getDestination(@PathVariable airportCode: String): ResponseEntity<DestinationDto> {
        val destination = contentService.getDestination(airportCode)
            ?: return ResponseEntity.notFound().build()
        if (!destination.isPublished) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(DestinationDto.from(destination))
    }
}
