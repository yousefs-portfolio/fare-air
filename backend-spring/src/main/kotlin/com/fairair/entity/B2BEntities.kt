package com.fairair.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Agency types.
 */
enum class AgencyType {
    TRAVEL_AGENT,
    CORPORATE,
    TOUR_OPERATOR
}

/**
 * Agency approval status.
 */
enum class AgencyStatus {
    PENDING,
    APPROVED,
    SUSPENDED,
    REJECTED
}

/**
 * Agency entity for B2B partners.
 */
@Table("agencies")
data class AgencyEntity(
    @Id
    val id: String,
    
    @Column("agency_code")
    val agencyCode: String,
    
    @Column("name")
    val name: String,
    
    @Column("name_ar")
    val nameAr: String? = null,
    
    @Column("type")
    val type: String,
    
    @Column("contact_name")
    val contactName: String,
    
    @Column("contact_email")
    val contactEmail: String,
    
    @Column("contact_phone")
    val contactPhone: String? = null,
    
    @Column("address")
    val address: String? = null,
    
    @Column("city")
    val city: String? = null,
    
    @Column("country")
    val country: String? = null,
    
    @Column("tax_id")
    val taxId: String? = null,
    
    @Column("license_number")
    val licenseNumber: String? = null,
    
    @Column("commission_rate")
    val commissionRate: BigDecimal = BigDecimal.ZERO,
    
    @Column("credit_limit")
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    
    @Column("current_balance")
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    
    @Column("status")
    val status: String = AgencyStatus.PENDING.name,
    
    @Column("approved_by")
    val approvedBy: String? = null,
    
    @Column("approved_at")
    val approvedAt: Instant? = null,
    
    @Column("notes")
    val notes: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Agency user roles.
 */
enum class AgencyUserRole {
    AGENCY_ADMIN,
    AGENT
}

/**
 * Agency user entity for B2B portal users.
 */
@Table("agency_users")
data class AgencyUserEntity(
    @Id
    val id: String,
    
    @Column("agency_id")
    val agencyId: String,
    
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
    
    @Column("phone")
    val phone: String? = null,
    
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
 * Group booking request status.
 */
enum class GroupBookingStatus {
    PENDING,
    QUOTED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED
}

/**
 * Group booking request entity.
 */
@Table("group_booking_requests")
data class GroupBookingRequestEntity(
    @Id
    val id: String,
    
    @Column("request_number")
    val requestNumber: String,
    
    @Column("agency_id")
    val agencyId: String? = null,
    
    @Column("contact_name")
    val contactName: String,
    
    @Column("contact_email")
    val contactEmail: String,
    
    @Column("contact_phone")
    val contactPhone: String,
    
    @Column("company_name")
    val companyName: String? = null,
    
    @Column("origin")
    val origin: String,
    
    @Column("destination")
    val destination: String,
    
    @Column("departure_date")
    val departureDate: LocalDate,
    
    @Column("return_date")
    val returnDate: LocalDate? = null,
    
    @Column("passenger_count")
    val passengerCount: Int,
    
    @Column("trip_type")
    val tripType: String,
    
    @Column("fare_class_preference")
    val fareClassPreference: String? = null,
    
    @Column("special_requirements")
    val specialRequirements: String? = null,
    
    @Column("status")
    val status: String = GroupBookingStatus.PENDING.name,
    
    @Column("quoted_amount")
    val quotedAmount: BigDecimal? = null,
    
    @Column("quoted_currency")
    val quotedCurrency: String? = null,
    
    @Column("quoted_at")
    val quotedAt: Instant? = null,
    
    @Column("quoted_by")
    val quotedBy: String? = null,
    
    @Column("quote_valid_until")
    val quoteValidUntil: Instant? = null,
    
    @Column("booking_pnr")
    val bookingPnr: String? = null,
    
    @Column("assigned_to")
    val assignedTo: String? = null,
    
    @Column("notes")
    val notes: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Charter types.
 */
enum class CharterType {
    SPORTS_TEAM,
    CORPORATE,
    HAJJ_UMRAH,
    SPECIAL_EVENT,
    OTHER
}

/**
 * Charter request entity.
 */
@Table("charter_requests")
data class CharterRequestEntity(
    @Id
    val id: String,
    
    @Column("request_number")
    val requestNumber: String,
    
    @Column("agency_id")
    val agencyId: String? = null,
    
    @Column("contact_name")
    val contactName: String,
    
    @Column("contact_email")
    val contactEmail: String,
    
    @Column("contact_phone")
    val contactPhone: String,
    
    @Column("company_name")
    val companyName: String? = null,
    
    @Column("charter_type")
    val charterType: String,
    
    @Column("origin")
    val origin: String,
    
    @Column("destination")
    val destination: String,
    
    @Column("departure_date")
    val departureDate: LocalDate,
    
    @Column("return_date")
    val returnDate: LocalDate? = null,
    
    @Column("passenger_count")
    val passengerCount: Int,
    
    @Column("aircraft_preference")
    val aircraftPreference: String? = null,
    
    @Column("catering_requirements")
    val cateringRequirements: String? = null,
    
    @Column("special_requirements")
    val specialRequirements: String? = null,
    
    @Column("status")
    val status: String = GroupBookingStatus.PENDING.name,
    
    @Column("quoted_amount")
    val quotedAmount: BigDecimal? = null,
    
    @Column("quoted_currency")
    val quotedCurrency: String? = null,
    
    @Column("quoted_at")
    val quotedAt: Instant? = null,
    
    @Column("quoted_by")
    val quotedBy: String? = null,
    
    @Column("quote_valid_until")
    val quoteValidUntil: Instant? = null,
    
    @Column("assigned_to")
    val assignedTo: String? = null,
    
    @Column("notes")
    val notes: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Agency booking entity for tracking B2B commissions.
 */
@Table("agency_bookings")
data class AgencyBookingEntity(
    @Id
    val id: String,
    
    @Column("agency_id")
    val agencyId: String,
    
    @Column("agent_user_id")
    val agentUserId: String,
    
    @Column("booking_pnr")
    val bookingPnr: String,
    
    @Column("commission_amount")
    val commissionAmount: BigDecimal,
    
    @Column("commission_status")
    val commissionStatus: String = "PENDING",
    
    @Column("payment_type")
    val paymentType: String,
    
    @Column("invoice_number")
    val invoiceNumber: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
