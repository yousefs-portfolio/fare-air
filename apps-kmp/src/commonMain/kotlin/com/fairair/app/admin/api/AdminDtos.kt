package com.fairair.app.admin.api

import kotlinx.serialization.Serializable

// ============================================================================
// AUTHENTICATION DTOs
// ============================================================================

@Serializable
data class AdminLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AdminLoginResponse(
    val success: Boolean,
    val message: String,
    val admin: AdminDto? = null,
    val token: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

// ============================================================================
// ADMIN USER DTOs
// ============================================================================

@Serializable
data class AdminDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val department: String? = null,
    val isActive: Boolean = true
) {
    val fullName: String get() = "$firstName $lastName"
    
    val roleDisplayName: String get() = when (role) {
        "SUPER_ADMIN" -> "Super Admin"
        "CONTENT_MANAGER" -> "Content Manager"
        "MARKETING" -> "Marketing"
        "B2B_SALES" -> "B2B Sales"
        else -> role
    }
}

@Serializable
data class CreateAdminRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val department: String? = null
)

// ============================================================================
// STATIC PAGE DTOs
// ============================================================================

@Serializable
data class StaticPageDto(
    val id: String,
    val slug: String,
    val title: String,
    val titleAr: String? = null,
    val content: String,
    val contentAr: String? = null,
    val metaDescription: String? = null,
    val metaDescriptionAr: String? = null,
    val isPublished: Boolean = false,
    val publishedAt: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreatePageRequest(
    val slug: String,
    val title: String,
    val content: String,
    val titleAr: String? = null,
    val contentAr: String? = null,
    val metaDescription: String? = null,
    val metaDescriptionAr: String? = null,
    val createdBy: String
)

@Serializable
data class UpdatePageRequest(
    val title: String? = null,
    val content: String? = null,
    val titleAr: String? = null,
    val contentAr: String? = null,
    val metaDescription: String? = null,
    val metaDescriptionAr: String? = null,
    val updatedBy: String
)

// ============================================================================
// LEGAL DOCUMENT DTOs
// ============================================================================

@Serializable
data class LegalDocumentDto(
    val id: String,
    val type: String,
    val language: String = "en",
    val version: String,
    val title: String,
    val content: String,
    val effectiveDate: String? = null,
    val isPublished: Boolean = false,
    val isCurrent: Boolean = false,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val lastUpdated: String? = null
) {
    val typeDisplayName: String get() = when (type) {
        "PRIVACY_POLICY" -> "Privacy Policy"
        "TERMS_OF_USE" -> "Terms of Use"
        "TERMS_OF_SERVICE" -> "Terms of Service"
        "COOKIE_POLICY" -> "Cookie Policy"
        "CONDITIONS_OF_CARRIAGE" -> "Conditions of Carriage"
        "BAGGAGE_POLICY" -> "Baggage Policy"
        "REFUND_POLICY" -> "Refund Policy"
        "CARRIER_REGULATIONS" -> "Carrier Regulations"
        "CUSTOMER_RIGHTS" -> "Customer Rights"
        "PRINCIPLE_OF_USE" -> "Principle of Use"
        else -> type
    }
}

@Serializable
data class CreateLegalDocumentRequest(
    val type: String,
    val language: String = "en",
    val version: String,
    val title: String,
    val content: String,
    val effectiveDate: String? = null,
    val createdBy: String? = null,
    val makeCurrent: Boolean = false
)

@Serializable
data class UpdateLegalDocumentRequest(
    val title: String? = null,
    val content: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null,
    val updatedBy: String? = null
)

// ============================================================================
// PROMOTION DTOs
// ============================================================================

@Serializable
data class PromotionDto(
    val id: String,
    val code: String,
    val title: String,
    val titleAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val discountType: String,
    val discountValue: String,
    val currency: String = "SAR",
    val minPurchaseAmount: String? = null,
    val maxDiscountAmount: String? = null,
    val originCode: String? = null,
    val destinationCode: String? = null,
    val fareFamily: String? = null,
    val imageUrl: String? = null,
    val startDate: String,
    val endDate: String,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val isActive: Boolean = true,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
) {
    val discountDisplayText: String get() = when (discountType) {
        "PERCENTAGE" -> "$discountValue% off"
        "FIXED_AMOUNT" -> "$currency $discountValue off"
        else -> discountValue
    }
}

@Serializable
data class CreatePromotionRequest(
    val code: String,
    val title: String,
    val discountType: String,
    val discountValue: String,
    val startDate: String,
    val endDate: String,
    val createdBy: String,
    val titleAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val currency: String = "SAR",
    val minPurchaseAmount: String? = null,
    val maxDiscountAmount: String? = null,
    val originCode: String? = null,
    val destinationCode: String? = null,
    val fareFamily: String? = null,
    val imageUrl: String? = null,
    val maxUses: Int? = null
)

@Serializable
data class UpdatePromotionRequest(
    val title: String? = null,
    val titleAr: String? = null,
    val description: String? = null,
    val descriptionAr: String? = null,
    val discountValue: String? = null,
    val minPurchaseAmount: String? = null,
    val maxDiscountAmount: String? = null,
    val imageUrl: String? = null,
    val endDate: String? = null,
    val maxUses: Int? = null,
    val updatedBy: String
)

// ============================================================================
// DESTINATION CONTENT DTOs
// ============================================================================

@Serializable
data class DestinationContentDto(
    val id: String,
    val airportCode: String,
    val titleEn: String,
    val titleAr: String? = null,
    val descriptionEn: String? = null,
    val descriptionAr: String? = null,
    val imageUrl: String? = null,
    val galleryUrls: String? = null,
    val lowestFare: String? = null,
    val currency: String = "SAR",
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
    val isPublished: Boolean = true,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateDestinationRequest(
    val airportCode: String,
    val titleEn: String,
    val titleAr: String? = null,
    val descriptionEn: String? = null,
    val descriptionAr: String? = null,
    val imageUrl: String? = null,
    val lowestFare: String? = null,
    val currency: String = "SAR",
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
    val createdBy: String? = null
)

@Serializable
data class UpdateDestinationRequest(
    val titleEn: String? = null,
    val titleAr: String? = null,
    val descriptionEn: String? = null,
    val descriptionAr: String? = null,
    val imageUrl: String? = null,
    val lowestFare: String? = null,
    val currency: String? = null,
    val isFeatured: Boolean? = null,
    val displayOrder: Int? = null,
    val updatedBy: String? = null
)

// ============================================================================
// AGENCY DTOs (B2B)
// ============================================================================

@Serializable
data class AgencyDto(
    val id: String,
    val agencyCode: String,
    val name: String,
    val nameAr: String? = null,
    val type: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val taxId: String? = null,
    val licenseNumber: String? = null,
    val status: String,
    val commissionRate: String? = null,
    val creditLimit: String? = null,
    val currentBalance: String? = null,
    val notes: String? = null,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val statusDisplayName: String get() = when (status) {
        "PENDING" -> "Pending Approval"
        "APPROVED" -> "Approved"
        "SUSPENDED" -> "Suspended"
        "REJECTED" -> "Rejected"
        else -> status
    }
    
    val typeDisplayName: String get() = when (type) {
        "TRAVEL_AGENT" -> "Travel Agent"
        "CORPORATE" -> "Corporate"
        "TOUR_OPERATOR" -> "Tour Operator"
        "CONSOLIDATOR" -> "Consolidator"
        else -> type
    }
}

@Serializable
data class ApproveAgencyRequest(
    val approvedBy: String,
    val commissionRate: String? = null,
    val creditLimit: String? = null
)

// ============================================================================
// GROUP BOOKING DTOs
// ============================================================================

@Serializable
data class GroupBookingRequestDto(
    val id: String,
    val requestNumber: String,
    val agencyId: String? = null,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val companyName: String? = null,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,
    val passengerCount: Int,
    val tripType: String,
    val fareClassPreference: String? = null,
    val specialRequirements: String? = null,
    val status: String,
    val assignedTo: String? = null,
    val quotedAmount: String? = null,
    val quotedCurrency: String? = null,
    val quotedAt: String? = null,
    val quotedBy: String? = null,
    val quoteValidUntil: String? = null,
    val bookingPnr: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val statusDisplayName: String get() = when (status) {
        "PENDING" -> "Pending"
        "QUOTED" -> "Quote Sent"
        "ACCEPTED" -> "Accepted"
        "REJECTED" -> "Rejected"
        "CANCELLED" -> "Cancelled"
        "COMPLETED" -> "Completed"
        else -> status
    }
}

@Serializable
data class SubmitQuoteRequest(
    val quotedAmount: String,
    val quotedCurrency: String = "SAR",
    val quotedBy: String,
    val validDays: Int = 7
)

// ============================================================================
// CHARTER REQUEST DTOs
// ============================================================================

@Serializable
data class CharterRequestDto(
    val id: String,
    val requestNumber: String,
    val agencyId: String? = null,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val companyName: String? = null,
    val charterType: String,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,
    val passengerCount: Int,
    val aircraftPreference: String? = null,
    val cateringRequirements: String? = null,
    val specialRequirements: String? = null,
    val status: String,
    val assignedTo: String? = null,
    val quotedAmount: String? = null,
    val quotedCurrency: String? = null,
    val quotedAt: String? = null,
    val quotedBy: String? = null,
    val quoteValidUntil: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val charterTypeDisplayName: String get() = when (charterType) {
        "FULL_CHARTER" -> "Full Charter"
        "HAJJ_UMRAH" -> "Hajj/Umrah"
        "SPORTS_TEAM" -> "Sports Team"
        "CORPORATE" -> "Corporate Event"
        "GOVERNMENT" -> "Government"
        "MILITARY" -> "Military"
        "OTHER" -> "Other"
        else -> charterType
    }
}
