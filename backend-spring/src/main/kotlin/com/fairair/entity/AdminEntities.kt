package com.fairair.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Admin user roles.
 */
enum class AdminRole {
    SUPER_ADMIN,
    CONTENT_MANAGER,
    MARKETING,
    B2B_SALES
}

/**
 * Admin user entity.
 */
@Table("admin_users")
data class AdminUserEntity(
    @Id
    val id: String,
    
    @Column("email")
    val email: String,
    
    @Column("password_hash")
    val passwordHash: String,
    
    @Column("first_name")
    val firstName: String,
    
    @Column("last_name")
    val lastName: String,
    
    @Column("role")
    val role: String,
    
    @Column("department")
    val department: String? = null,
    
    @Column("is_active")
    val isActive: Boolean = true,
    
    @Column("last_login_at")
    val lastLoginAt: Instant? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Static page entity for CMS content.
 */
@Table("static_pages")
data class StaticPageEntity(
    @Id
    val id: String,
    
    @Column("slug")
    val slug: String,
    
    @Column("title")
    val title: String,
    
    @Column("title_ar")
    val titleAr: String? = null,
    
    @Column("content")
    val content: String,
    
    @Column("content_ar")
    val contentAr: String? = null,
    
    @Column("meta_description")
    val metaDescription: String? = null,
    
    @Column("meta_description_ar")
    val metaDescriptionAr: String? = null,
    
    @Column("is_published")
    val isPublished: Boolean = false,
    
    @Column("published_at")
    val publishedAt: Instant? = null,
    
    @Column("created_by")
    val createdBy: String,
    
    @Column("updated_by")
    val updatedBy: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Legal document types.
 */
enum class LegalDocumentType {
    PRIVACY_POLICY,
    TERMS_OF_USE,
    COOKIE_POLICY,
    CARRIER_REGULATIONS,
    CONDITIONS_OF_CARRIAGE,
    CUSTOMER_RIGHTS,
    PRINCIPLE_OF_USE
}

/**
 * Legal document entity for versioned legal content.
 */
@Table("legal_documents")
data class LegalDocumentEntity(
    @Id
    val id: String,
    
    @Column("type")
    val type: String,
    
    @Column("version")
    val version: String,
    
    @Column("title")
    val title: String,
    
    @Column("title_ar")
    val titleAr: String? = null,
    
    @Column("content")
    val content: String,
    
    @Column("content_ar")
    val contentAr: String? = null,
    
    @Column("effective_date")
    val effectiveDate: LocalDate,
    
    @Column("is_current")
    val isCurrent: Boolean = false,
    
    @Column("created_by")
    val createdBy: String,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)

/**
 * Promotion discount types.
 */
enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT
}

/**
 * Promotion entity for marketing campaigns.
 */
@Table("promotions")
data class PromotionEntity(
    @Id
    val id: String,
    
    @Column("code")
    val code: String? = null,
    
    @Column("title")
    val title: String,
    
    @Column("title_ar")
    val titleAr: String? = null,
    
    @Column("description")
    val description: String? = null,
    
    @Column("description_ar")
    val descriptionAr: String? = null,
    
    @Column("discount_type")
    val discountType: String,
    
    @Column("discount_value")
    val discountValue: BigDecimal,
    
    @Column("currency")
    val currency: String = "SAR",
    
    @Column("min_purchase_amount")
    val minPurchaseAmount: BigDecimal? = null,
    
    @Column("max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null,
    
    @Column("origin_code")
    val originCode: String? = null,
    
    @Column("destination_code")
    val destinationCode: String? = null,
    
    @Column("fare_family")
    val fareFamily: String? = null,
    
    @Column("image_url")
    val imageUrl: String? = null,
    
    @Column("start_date")
    val startDate: Instant,
    
    @Column("end_date")
    val endDate: Instant,
    
    @Column("max_uses")
    val maxUses: Int? = null,
    
    @Column("current_uses")
    val currentUses: Int = 0,
    
    @Column("is_active")
    val isActive: Boolean = true,
    
    @Column("created_by")
    val createdBy: String,
    
    @Column("updated_by")
    val updatedBy: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Destination content entity for destination pages.
 */
@Table("destination_content")
data class DestinationContentEntity(
    @Id
    val id: String,
    
    @Column("airport_code")
    val airportCode: String,
    
    @Column("city_name")
    val cityName: String,
    
    @Column("city_name_ar")
    val cityNameAr: String? = null,
    
    @Column("country")
    val country: String,
    
    @Column("country_ar")
    val countryAr: String? = null,
    
    @Column("description")
    val description: String? = null,
    
    @Column("description_ar")
    val descriptionAr: String? = null,
    
    @Column("highlights")
    val highlights: String? = null, // JSON array
    
    @Column("highlights_ar")
    val highlightsAr: String? = null,
    
    @Column("image_url")
    val imageUrl: String? = null,
    
    @Column("gallery_urls")
    val galleryUrls: String? = null, // JSON array
    
    @Column("lowest_fare")
    val lowestFare: BigDecimal? = null,
    
    @Column("is_featured")
    val isFeatured: Boolean = false,
    
    @Column("is_published")
    val isPublished: Boolean = true,
    
    @Column("created_by")
    val createdBy: String,
    
    @Column("updated_by")
    val updatedBy: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
