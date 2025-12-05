package com.fairair.service

import com.fairair.entity.*
import com.fairair.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

// ============================================================================
// PROMOTION SERVICE
// ============================================================================

@Service
class PromotionService(
    private val promotionRepository: PromotionRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    suspend fun getPromotionByCode(code: String): PromotionEntity? {
        return promotionRepository.findByCode(code)
    }
    
    suspend fun validateAndApplyPromotion(
        code: String,
        amount: BigDecimal,
        origin: String? = null,
        destination: String? = null,
        fareFamily: String? = null
    ): PromotionResult {
        val promotion = promotionRepository.findByCode(code)
            ?: return PromotionResult(false, "Invalid promotion code", BigDecimal.ZERO)
        
        if (!promotion.isActive) {
            return PromotionResult(false, "Promotion is not active", BigDecimal.ZERO)
        }
        
        val now = Instant.now()
        if (now.isBefore(promotion.startDate) || now.isAfter(promotion.endDate)) {
            return PromotionResult(false, "Promotion has expired or not yet started", BigDecimal.ZERO)
        }
        
        if (promotion.maxUses != null && promotion.currentUses >= promotion.maxUses) {
            return PromotionResult(false, "Promotion usage limit reached", BigDecimal.ZERO)
        }
        
        if (promotion.minPurchaseAmount != null && amount < promotion.minPurchaseAmount) {
            return PromotionResult(false, "Minimum purchase amount not met", BigDecimal.ZERO)
        }
        
        // Check route restrictions
        if (promotion.originCode != null && promotion.originCode != origin) {
            return PromotionResult(false, "Promotion not valid for this origin", BigDecimal.ZERO)
        }
        if (promotion.destinationCode != null && promotion.destinationCode != destination) {
            return PromotionResult(false, "Promotion not valid for this destination", BigDecimal.ZERO)
        }
        if (promotion.fareFamily != null && promotion.fareFamily != fareFamily) {
            return PromotionResult(false, "Promotion not valid for this fare type", BigDecimal.ZERO)
        }
        
        // Calculate discount
        val discount = when (promotion.discountType) {
            DiscountType.PERCENTAGE.name -> {
                val calculated = amount.multiply(promotion.discountValue.divide(BigDecimal(100)))
                if (promotion.maxDiscountAmount != null && calculated > promotion.maxDiscountAmount) {
                    promotion.maxDiscountAmount
                } else {
                    calculated
                }
            }
            DiscountType.FIXED_AMOUNT.name -> {
                minOf(promotion.discountValue, amount)
            }
            else -> BigDecimal.ZERO
        }
        
        return PromotionResult(true, "Promotion applied", discount, promotion)
    }
    
    suspend fun incrementUsage(promotionId: String): Boolean {
        val promotion = promotionRepository.findById(promotionId) ?: return false
        promotionRepository.save(promotion.copy(
            currentUses = promotion.currentUses + 1,
            updatedAt = Instant.now()
        ))
        return true
    }
    
    fun getActivePromotions(): Flow<PromotionEntity> {
        return promotionRepository.findByIsActiveTrue()
    }
    
    fun getAllPromotions(): Flow<PromotionEntity> {
        return promotionRepository.findAllByOrderByStartDateDesc()
    }
    
    suspend fun createPromotion(
        title: String,
        discountType: DiscountType,
        discountValue: BigDecimal,
        startDate: Instant,
        endDate: Instant,
        createdBy: String,
        code: String? = null,
        titleAr: String? = null,
        description: String? = null,
        descriptionAr: String? = null,
        currency: String = "SAR",
        minPurchaseAmount: BigDecimal? = null,
        maxDiscountAmount: BigDecimal? = null,
        originCode: String? = null,
        destinationCode: String? = null,
        fareFamily: String? = null,
        imageUrl: String? = null,
        maxUses: Int? = null
    ): PromotionEntity {
        // Validate code uniqueness if provided
        if (code != null) {
            val existing = promotionRepository.findByCode(code)
            if (existing != null) {
                throw IllegalArgumentException("Promotion code '$code' already exists")
            }
        }
        
        if (endDate.isBefore(startDate)) {
            throw IllegalArgumentException("End date must be after start date")
        }
        
        val promotion = PromotionEntity(
            id = UUID.randomUUID().toString(),
            code = code,
            title = title,
            titleAr = titleAr,
            description = description,
            descriptionAr = descriptionAr,
            discountType = discountType.name,
            discountValue = discountValue,
            currency = currency,
            minPurchaseAmount = minPurchaseAmount,
            maxDiscountAmount = maxDiscountAmount,
            originCode = originCode,
            destinationCode = destinationCode,
            fareFamily = fareFamily,
            imageUrl = imageUrl,
            startDate = startDate,
            endDate = endDate,
            maxUses = maxUses,
            currentUses = 0,
            isActive = true,
            createdBy = createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(promotion).awaitSingle()
    }
    
    suspend fun updatePromotion(
        id: String,
        updatedBy: String,
        title: String? = null,
        titleAr: String? = null,
        description: String? = null,
        descriptionAr: String? = null,
        discountValue: BigDecimal? = null,
        minPurchaseAmount: BigDecimal? = null,
        maxDiscountAmount: BigDecimal? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        maxUses: Int? = null,
        isActive: Boolean? = null,
        imageUrl: String? = null
    ): PromotionEntity? {
        val promotion = promotionRepository.findById(id) ?: return null
        
        val updated = promotion.copy(
            title = title ?: promotion.title,
            titleAr = titleAr ?: promotion.titleAr,
            description = description ?: promotion.description,
            descriptionAr = descriptionAr ?: promotion.descriptionAr,
            discountValue = discountValue ?: promotion.discountValue,
            minPurchaseAmount = minPurchaseAmount ?: promotion.minPurchaseAmount,
            maxDiscountAmount = maxDiscountAmount ?: promotion.maxDiscountAmount,
            startDate = startDate ?: promotion.startDate,
            endDate = endDate ?: promotion.endDate,
            maxUses = maxUses ?: promotion.maxUses,
            isActive = isActive ?: promotion.isActive,
            imageUrl = imageUrl ?: promotion.imageUrl,
            updatedBy = updatedBy,
            updatedAt = Instant.now()
        )
        
        return promotionRepository.save(updated)
    }
    
    suspend fun deactivatePromotion(id: String): Boolean {
        val promotion = promotionRepository.findById(id) ?: return false
        promotionRepository.save(promotion.copy(isActive = false, updatedAt = Instant.now()))
        return true
    }
    
    suspend fun deletePromotion(id: String): Boolean {
        val promotion = promotionRepository.findById(id) ?: return false
        promotionRepository.delete(promotion)
        return true
    }
}

data class PromotionResult(
    val isValid: Boolean,
    val message: String,
    val discountAmount: BigDecimal,
    val promotion: PromotionEntity? = null
)

// ============================================================================
// AGENCY SERVICE (B2B)
// ============================================================================

@Service
class AgencyService(
    private val agencyRepository: AgencyRepository,
    private val agencyUserRepository: AgencyUserRepository,
    private val agencyBookingRepository: AgencyBookingRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
    private val passwordEncoder: org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
) {
    private fun generateAgencyCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random(System.currentTimeMillis())
        val code = (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return "AGN$code"
    }
    
    // ----------------------
    // AGENCY MANAGEMENT
    // ----------------------
    
    suspend fun registerAgency(
        name: String,
        type: AgencyType,
        contactName: String,
        contactEmail: String,
        contactPhone: String? = null,
        address: String? = null,
        city: String? = null,
        country: String? = null,
        taxId: String? = null,
        licenseNumber: String? = null,
        nameAr: String? = null
    ): AgencyEntity {
        // Check if email already registered
        val existing = agencyRepository.findByContactEmail(contactEmail)
        if (existing != null) {
            throw IllegalArgumentException("An agency with this email already exists")
        }
        
        val agency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = generateAgencyCode(),
            name = name,
            nameAr = nameAr,
            type = type.name,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            address = address,
            city = city,
            country = country,
            taxId = taxId,
            licenseNumber = licenseNumber,
            status = AgencyStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(agency).awaitSingle()
    }
    
    suspend fun getAgencyById(id: String): AgencyEntity? {
        return agencyRepository.findById(id)
    }
    
    suspend fun getAgencyByCode(code: String): AgencyEntity? {
        return agencyRepository.findByAgencyCode(code)
    }
    
    fun getAgenciesByStatus(status: AgencyStatus): Flow<AgencyEntity> {
        return agencyRepository.findByStatus(status.name)
    }
    
    fun getAgenciesByType(type: AgencyType): Flow<AgencyEntity> {
        return agencyRepository.findByType(type.name)
    }
    
    fun getAllAgencies(): Flow<AgencyEntity> {
        return agencyRepository.findAllByOrderByCreatedAtDesc()
    }
    
    suspend fun approveAgency(
        agencyId: String,
        approvedBy: String,
        commissionRate: BigDecimal = BigDecimal("5.0"),
        creditLimit: BigDecimal = BigDecimal.ZERO
    ): AgencyEntity? {
        val agency = agencyRepository.findById(agencyId) ?: return null
        
        val updated = agency.copy(
            status = AgencyStatus.APPROVED.name,
            approvedBy = approvedBy,
            approvedAt = Instant.now(),
            commissionRate = commissionRate,
            creditLimit = creditLimit,
            updatedAt = Instant.now()
        )
        
        return agencyRepository.save(updated)
    }
    
    suspend fun suspendAgency(agencyId: String, notes: String? = null): AgencyEntity? {
        val agency = agencyRepository.findById(agencyId) ?: return null
        
        val updated = agency.copy(
            status = AgencyStatus.SUSPENDED.name,
            notes = notes ?: agency.notes,
            updatedAt = Instant.now()
        )
        
        return agencyRepository.save(updated)
    }
    
    suspend fun rejectAgency(agencyId: String, notes: String? = null): AgencyEntity? {
        val agency = agencyRepository.findById(agencyId) ?: return null
        
        val updated = agency.copy(
            status = AgencyStatus.REJECTED.name,
            notes = notes ?: agency.notes,
            updatedAt = Instant.now()
        )
        
        return agencyRepository.save(updated)
    }
    
    suspend fun updateAgencyBalance(agencyId: String, amount: BigDecimal): AgencyEntity? {
        val agency = agencyRepository.findById(agencyId) ?: return null
        
        val updated = agency.copy(
            currentBalance = agency.currentBalance + amount,
            updatedAt = Instant.now()
        )
        
        return agencyRepository.save(updated)
    }
    
    // ----------------------
    // AGENCY USER MANAGEMENT
    // ----------------------
    
    suspend fun authenticateAgencyUser(email: String, password: String): AgencyUserEntity? {
        val user = agencyUserRepository.findByEmailIgnoreCase(email) ?: return null
        if (!user.isActive) return null
        if (!passwordEncoder.matches(password, user.passwordHash)) return null
        
        // Check agency status
        val agency = agencyRepository.findById(user.agencyId)
        if (agency == null || agency.status != AgencyStatus.APPROVED.name) {
            return null
        }
        
        // Update last login
        return agencyUserRepository.save(user.copy(
            lastLoginAt = Instant.now(),
            updatedAt = Instant.now()
        ))
    }
    
    suspend fun createAgencyUser(
        agencyId: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: AgencyUserRole,
        phone: String? = null
    ): AgencyUserEntity {
        val existing = agencyUserRepository.findByEmailIgnoreCase(email)
        if (existing != null) {
            throw IllegalArgumentException("User with this email already exists")
        }
        
        val user = AgencyUserEntity(
            id = UUID.randomUUID().toString(),
            agencyId = agencyId,
            email = email.lowercase(),
            passwordHash = passwordEncoder.encode(password),
            firstName = firstName,
            lastName = lastName,
            role = role.name,
            phone = phone,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(user).awaitSingle()
    }
    
    fun getAgencyUsers(agencyId: String): Flow<AgencyUserEntity> {
        return agencyUserRepository.findByAgencyId(agencyId)
    }
    
    fun getActiveAgencyUsers(agencyId: String): Flow<AgencyUserEntity> {
        return agencyUserRepository.findByAgencyIdAndIsActiveTrue(agencyId)
    }
    
    suspend fun deactivateAgencyUser(userId: String): Boolean {
        val user = agencyUserRepository.findById(userId) ?: return false
        agencyUserRepository.save(user.copy(isActive = false, updatedAt = Instant.now()))
        return true
    }
    
    // ----------------------
    // AGENCY BOOKINGS
    // ----------------------
    
    fun getAgencyBookings(agencyId: String): Flow<AgencyBookingEntity> {
        return agencyBookingRepository.findByAgencyId(agencyId)
    }
    
    fun getAgentBookings(agentUserId: String): Flow<AgencyBookingEntity> {
        return agencyBookingRepository.findByAgentUserId(agentUserId)
    }
    
    suspend fun recordAgencyBooking(
        agencyId: String,
        agentUserId: String,
        bookingPnr: String,
        commissionAmount: BigDecimal,
        paymentType: String
    ): AgencyBookingEntity {
        val booking = AgencyBookingEntity(
            id = UUID.randomUUID().toString(),
            agencyId = agencyId,
            agentUserId = agentUserId,
            bookingPnr = bookingPnr,
            commissionAmount = commissionAmount,
            commissionStatus = "PENDING",
            paymentType = paymentType,
            createdAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(booking).awaitSingle()
    }
}

// ============================================================================
// GROUP BOOKING SERVICE
// ============================================================================

@Service
class GroupBookingService(
    private val groupBookingRequestRepository: GroupBookingRequestRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    private fun generateRequestNumber(): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val random = (1000..9999).random()
        return "GRP-$date-$random"
    }
    
    suspend fun createRequest(
        contactName: String,
        contactEmail: String,
        contactPhone: String,
        origin: String,
        destination: String,
        departureDate: LocalDate,
        passengerCount: Int,
        tripType: String,
        agencyId: String? = null,
        companyName: String? = null,
        returnDate: LocalDate? = null,
        fareClassPreference: String? = null,
        specialRequirements: String? = null
    ): GroupBookingRequestEntity {
        if (passengerCount < 10) {
            throw IllegalArgumentException("Group bookings require minimum 10 passengers")
        }
        
        val request = GroupBookingRequestEntity(
            id = UUID.randomUUID().toString(),
            requestNumber = generateRequestNumber(),
            agencyId = agencyId,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            companyName = companyName,
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            passengerCount = passengerCount,
            tripType = tripType,
            fareClassPreference = fareClassPreference,
            specialRequirements = specialRequirements,
            status = GroupBookingStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(request).awaitSingle()
    }
    
    suspend fun getRequestByNumber(requestNumber: String): GroupBookingRequestEntity? {
        return groupBookingRequestRepository.findByRequestNumber(requestNumber)
    }
    
    suspend fun getRequestById(id: String): GroupBookingRequestEntity? {
        return groupBookingRequestRepository.findById(id)
    }
    
    fun getAgencyRequests(agencyId: String): Flow<GroupBookingRequestEntity> {
        return groupBookingRequestRepository.findByAgencyId(agencyId)
    }
    
    fun getRequestsByStatus(status: GroupBookingStatus): Flow<GroupBookingRequestEntity> {
        return groupBookingRequestRepository.findByStatus(status.name)
    }
    
    fun getAssignedRequests(adminId: String): Flow<GroupBookingRequestEntity> {
        return groupBookingRequestRepository.findByAssignedTo(adminId)
    }
    
    fun getAllRequests(): Flow<GroupBookingRequestEntity> {
        return groupBookingRequestRepository.findAllByOrderByCreatedAtDesc()
    }
    
    suspend fun assignRequest(requestId: String, assignedTo: String): GroupBookingRequestEntity? {
        val request = groupBookingRequestRepository.findById(requestId) ?: return null
        return groupBookingRequestRepository.save(request.copy(
            assignedTo = assignedTo,
            updatedAt = Instant.now()
        ))
    }
    
    suspend fun submitQuote(
        requestId: String,
        quotedAmount: BigDecimal,
        quotedCurrency: String,
        quotedBy: String,
        validUntil: Instant
    ): GroupBookingRequestEntity? {
        val request = groupBookingRequestRepository.findById(requestId) ?: return null
        
        return groupBookingRequestRepository.save(request.copy(
            status = GroupBookingStatus.QUOTED.name,
            quotedAmount = quotedAmount,
            quotedCurrency = quotedCurrency,
            quotedAt = Instant.now(),
            quotedBy = quotedBy,
            quoteValidUntil = validUntil,
            updatedAt = Instant.now()
        ))
    }
    
    suspend fun updateStatus(
        requestId: String,
        status: GroupBookingStatus,
        notes: String? = null,
        bookingPnr: String? = null
    ): GroupBookingRequestEntity? {
        val request = groupBookingRequestRepository.findById(requestId) ?: return null
        
        return groupBookingRequestRepository.save(request.copy(
            status = status.name,
            notes = notes ?: request.notes,
            bookingPnr = bookingPnr ?: request.bookingPnr,
            updatedAt = Instant.now()
        ))
    }
}

// ============================================================================
// CHARTER REQUEST SERVICE
// ============================================================================

@Service
class CharterRequestService(
    private val charterRequestRepository: CharterRequestRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    private fun generateRequestNumber(): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val random = (1000..9999).random()
        return "CHR-$date-$random"
    }
    
    suspend fun createRequest(
        contactName: String,
        contactEmail: String,
        contactPhone: String,
        charterType: CharterType,
        origin: String,
        destination: String,
        departureDate: LocalDate,
        passengerCount: Int,
        agencyId: String? = null,
        companyName: String? = null,
        returnDate: LocalDate? = null,
        aircraftPreference: String? = null,
        cateringRequirements: String? = null,
        specialRequirements: String? = null
    ): CharterRequestEntity {
        val request = CharterRequestEntity(
            id = UUID.randomUUID().toString(),
            requestNumber = generateRequestNumber(),
            agencyId = agencyId,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            companyName = companyName,
            charterType = charterType.name,
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            passengerCount = passengerCount,
            aircraftPreference = aircraftPreference,
            cateringRequirements = cateringRequirements,
            specialRequirements = specialRequirements,
            status = GroupBookingStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return r2dbcEntityTemplate.insert(request).awaitSingle()
    }
    
    suspend fun getRequestByNumber(requestNumber: String): CharterRequestEntity? {
        return charterRequestRepository.findByRequestNumber(requestNumber)
    }
    
    suspend fun getRequestById(id: String): CharterRequestEntity? {
        return charterRequestRepository.findById(id)
    }
    
    fun getAgencyRequests(agencyId: String): Flow<CharterRequestEntity> {
        return charterRequestRepository.findByAgencyId(agencyId)
    }
    
    fun getRequestsByStatus(status: GroupBookingStatus): Flow<CharterRequestEntity> {
        return charterRequestRepository.findByStatus(status.name)
    }
    
    fun getRequestsByType(type: CharterType): Flow<CharterRequestEntity> {
        return charterRequestRepository.findByCharterType(type.name)
    }
    
    fun getAllRequests(): Flow<CharterRequestEntity> {
        return charterRequestRepository.findAllByOrderByCreatedAtDesc()
    }
    
    suspend fun assignRequest(requestId: String, assignedTo: String): CharterRequestEntity? {
        val request = charterRequestRepository.findById(requestId) ?: return null
        return charterRequestRepository.save(request.copy(
            assignedTo = assignedTo,
            updatedAt = Instant.now()
        ))
    }
    
    suspend fun submitQuote(
        requestId: String,
        quotedAmount: BigDecimal,
        quotedCurrency: String,
        quotedBy: String,
        validUntil: Instant
    ): CharterRequestEntity? {
        val request = charterRequestRepository.findById(requestId) ?: return null
        
        return charterRequestRepository.save(request.copy(
            status = GroupBookingStatus.QUOTED.name,
            quotedAmount = quotedAmount,
            quotedCurrency = quotedCurrency,
            quotedAt = Instant.now(),
            quotedBy = quotedBy,
            quoteValidUntil = validUntil,
            updatedAt = Instant.now()
        ))
    }
    
    suspend fun updateStatus(
        requestId: String,
        status: GroupBookingStatus,
        notes: String? = null
    ): CharterRequestEntity? {
        val request = charterRequestRepository.findById(requestId) ?: return null
        
        return charterRequestRepository.save(request.copy(
            status = status.name,
            notes = notes ?: request.notes,
            updatedAt = Instant.now()
        ))
    }
}
