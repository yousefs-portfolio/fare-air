package com.fairair.controller

import com.fairair.entity.*
import com.fairair.service.*
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// ============================================================================
// B2B AGENCY AUTHENTICATION CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/b2b/auth")
class B2BAuthController(
    private val agencyService: AgencyService
) {
    @PostMapping("/login")
    suspend fun login(@RequestBody request: B2BLoginRequest): ResponseEntity<B2BLoginResponse> {
        val user = agencyService.authenticateAgencyUser(request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(B2BLoginResponse(success = false, message = "Invalid credentials or agency not approved"))
        
        val agency = agencyService.getAgencyById(user.agencyId)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(B2BLoginResponse(success = false, message = "Agency not found"))
        
        return ResponseEntity.ok(B2BLoginResponse(
            success = true,
            message = "Login successful",
            user = AgencyUserDto.from(user),
            agency = AgencyDto.from(agency),
            token = generateToken(user) // TODO: Implement proper JWT
        ))
    }
    
    @PostMapping("/register-agency")
    suspend fun registerAgency(@RequestBody request: RegisterAgencyRequest): ResponseEntity<AgencyRegistrationResponse> {
        return try {
            val agency = agencyService.registerAgency(
                name = request.name,
                type = AgencyType.valueOf(request.type),
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                address = request.address,
                city = request.city,
                country = request.country,
                taxId = request.taxId,
                licenseNumber = request.licenseNumber,
                nameAr = request.nameAr
            )
            ResponseEntity.status(HttpStatus.CREATED).body(AgencyRegistrationResponse(
                success = true,
                message = "Agency registration submitted. Pending approval.",
                agency = AgencyDto.from(agency)
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(AgencyRegistrationResponse(
                success = false,
                message = e.message ?: "Registration failed"
            ))
        }
    }
    
    private fun generateToken(user: AgencyUserEntity): String {
        // TODO: Implement proper JWT token generation
        return "b2b-token-${user.id}"
    }
}

data class B2BLoginRequest(val email: String, val password: String)
data class B2BLoginResponse(
    val success: Boolean,
    val message: String,
    val user: AgencyUserDto? = null,
    val agency: AgencyDto? = null,
    val token: String? = null
)

data class RegisterAgencyRequest(
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
    val licenseNumber: String? = null
)

data class AgencyRegistrationResponse(
    val success: Boolean,
    val message: String,
    val agency: AgencyDto? = null
)

data class AgencyDto(
    val id: String,
    val agencyCode: String,
    val name: String,
    val nameAr: String?,
    val type: String,
    val contactName: String,
    val contactEmail: String,
    val status: String,
    val commissionRate: BigDecimal,
    val creditLimit: BigDecimal,
    val currentBalance: BigDecimal
) {
    companion object {
        fun from(entity: AgencyEntity) = AgencyDto(
            id = entity.id,
            agencyCode = entity.agencyCode,
            name = entity.name,
            nameAr = entity.nameAr,
            type = entity.type,
            contactName = entity.contactName,
            contactEmail = entity.contactEmail,
            status = entity.status,
            commissionRate = entity.commissionRate,
            creditLimit = entity.creditLimit,
            currentBalance = entity.currentBalance
        )
    }
}

data class AgencyUserDto(
    val id: String,
    val agencyId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val isActive: Boolean
) {
    companion object {
        fun from(entity: AgencyUserEntity) = AgencyUserDto(
            id = entity.id,
            agencyId = entity.agencyId,
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            role = entity.role,
            isActive = entity.isActive
        )
    }
}

// ============================================================================
// B2B AGENCY MANAGEMENT CONTROLLER (Admin)
// ============================================================================

@RestController
@RequestMapping("/api/admin/agencies")
class AgencyManagementController(
    private val agencyService: AgencyService
) {
    @GetMapping
    suspend fun getAllAgencies(): ResponseEntity<List<AgencyDto>> {
        val agencies = agencyService.getAllAgencies().toList()
        return ResponseEntity.ok(agencies.map { AgencyDto.from(it) })
    }
    
    @GetMapping("/pending")
    suspend fun getPendingAgencies(): ResponseEntity<List<AgencyDto>> {
        val agencies = agencyService.getAgenciesByStatus(AgencyStatus.PENDING).toList()
        return ResponseEntity.ok(agencies.map { AgencyDto.from(it) })
    }
    
    @GetMapping("/{id}")
    suspend fun getAgency(@PathVariable id: String): ResponseEntity<AgencyDto> {
        val agency = agencyService.getAgencyById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AgencyDto.from(agency))
    }
    
    @PostMapping("/{id}/approve")
    suspend fun approveAgency(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: ApproveAgencyRequest
    ): ResponseEntity<AgencyDto> {
        val agency = agencyService.approveAgency(
            agencyId = id,
            approvedBy = adminId,
            commissionRate = request.commissionRate,
            creditLimit = request.creditLimit
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AgencyDto.from(agency))
    }
    
    @PostMapping("/{id}/suspend")
    suspend fun suspendAgency(
        @PathVariable id: String,
        @RequestBody request: SuspendAgencyRequest
    ): ResponseEntity<AgencyDto> {
        val agency = agencyService.suspendAgency(id, request.notes)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AgencyDto.from(agency))
    }
    
    @PostMapping("/{id}/reject")
    suspend fun rejectAgency(
        @PathVariable id: String,
        @RequestBody request: RejectAgencyRequest
    ): ResponseEntity<AgencyDto> {
        val agency = agencyService.rejectAgency(id, request.notes)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AgencyDto.from(agency))
    }
    
    @GetMapping("/{id}/users")
    suspend fun getAgencyUsers(@PathVariable id: String): ResponseEntity<List<AgencyUserDto>> {
        val users = agencyService.getAgencyUsers(id).toList()
        return ResponseEntity.ok(users.map { AgencyUserDto.from(it) })
    }
    
    @GetMapping("/{id}/bookings")
    suspend fun getAgencyBookings(@PathVariable id: String): ResponseEntity<List<AgencyBookingDto>> {
        val bookings = agencyService.getAgencyBookings(id).toList()
        return ResponseEntity.ok(bookings.map { AgencyBookingDto.from(it) })
    }
}

data class ApproveAgencyRequest(
    val commissionRate: BigDecimal = BigDecimal("5.0"),
    val creditLimit: BigDecimal = BigDecimal.ZERO
)

data class SuspendAgencyRequest(val notes: String? = null)
data class RejectAgencyRequest(val notes: String? = null)

data class AgencyBookingDto(
    val id: String,
    val agencyId: String,
    val agentUserId: String,
    val bookingPnr: String,
    val commissionAmount: BigDecimal,
    val commissionStatus: String,
    val paymentType: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: AgencyBookingEntity) = AgencyBookingDto(
            id = entity.id,
            agencyId = entity.agencyId,
            agentUserId = entity.agentUserId,
            bookingPnr = entity.bookingPnr,
            commissionAmount = entity.commissionAmount,
            commissionStatus = entity.commissionStatus,
            paymentType = entity.paymentType,
            createdAt = entity.createdAt
        )
    }
}

// ============================================================================
// B2B PORTAL CONTROLLER (for agencies)
// ============================================================================

@RestController
@RequestMapping("/api/b2b/portal")
class B2BPortalController(
    private val agencyService: AgencyService
) {
    @GetMapping("/agency")
    suspend fun getMyAgency(
        @RequestHeader("X-Agency-Id") agencyId: String
    ): ResponseEntity<AgencyDto> {
        val agency = agencyService.getAgencyById(agencyId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(AgencyDto.from(agency))
    }
    
    @GetMapping("/users")
    suspend fun getAgencyUsers(
        @RequestHeader("X-Agency-Id") agencyId: String
    ): ResponseEntity<List<AgencyUserDto>> {
        val users = agencyService.getActiveAgencyUsers(agencyId).toList()
        return ResponseEntity.ok(users.map { AgencyUserDto.from(it) })
    }
    
    @PostMapping("/users")
    suspend fun createAgencyUser(
        @RequestHeader("X-Agency-Id") agencyId: String,
        @RequestBody request: CreateAgencyUserRequest
    ): ResponseEntity<AgencyUserDto> {
        return try {
            val user = agencyService.createAgencyUser(
                agencyId = agencyId,
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                role = AgencyUserRole.valueOf(request.role),
                phone = request.phone
            )
            ResponseEntity.status(HttpStatus.CREATED).body(AgencyUserDto.from(user))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @DeleteMapping("/users/{userId}")
    suspend fun deactivateUser(
        @PathVariable userId: String
    ): ResponseEntity<Void> {
        return if (agencyService.deactivateAgencyUser(userId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/bookings")
    suspend fun getAgencyBookings(
        @RequestHeader("X-Agency-Id") agencyId: String
    ): ResponseEntity<List<AgencyBookingDto>> {
        val bookings = agencyService.getAgencyBookings(agencyId).toList()
        return ResponseEntity.ok(bookings.map { AgencyBookingDto.from(it) })
    }
    
    @GetMapping("/bookings/my")
    suspend fun getMyBookings(
        @RequestHeader("X-Agent-Id") agentId: String
    ): ResponseEntity<List<AgencyBookingDto>> {
        val bookings = agencyService.getAgentBookings(agentId).toList()
        return ResponseEntity.ok(bookings.map { AgencyBookingDto.from(it) })
    }
}

data class CreateAgencyUserRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phone: String? = null
)

// ============================================================================
// GROUP BOOKING REQUEST CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/group-bookings")
class GroupBookingController(
    private val groupBookingService: GroupBookingService
) {
    // Public endpoint for submitting group booking requests
    @PostMapping
    suspend fun createRequest(@RequestBody request: CreateGroupBookingRequest): ResponseEntity<GroupBookingRequestDto> {
        return try {
            val groupRequest = groupBookingService.createRequest(
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                origin = request.origin,
                destination = request.destination,
                departureDate = request.departureDate,
                passengerCount = request.passengerCount,
                tripType = request.tripType,
                agencyId = request.agencyId,
                companyName = request.companyName,
                returnDate = request.returnDate,
                fareClassPreference = request.fareClassPreference,
                specialRequirements = request.specialRequirements
            )
            ResponseEntity.status(HttpStatus.CREATED).body(GroupBookingRequestDto.from(groupRequest))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/{requestNumber}")
    suspend fun getRequest(@PathVariable requestNumber: String): ResponseEntity<GroupBookingRequestDto> {
        val request = groupBookingService.getRequestByNumber(requestNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(GroupBookingRequestDto.from(request))
    }
}

@RestController
@RequestMapping("/api/admin/group-bookings")
class AdminGroupBookingController(
    private val groupBookingService: GroupBookingService
) {
    @GetMapping
    suspend fun getAllRequests(): ResponseEntity<List<GroupBookingRequestDto>> {
        val requests = groupBookingService.getAllRequests().toList()
        return ResponseEntity.ok(requests.map { GroupBookingRequestDto.from(it) })
    }
    
    @GetMapping("/pending")
    suspend fun getPendingRequests(): ResponseEntity<List<GroupBookingRequestDto>> {
        val requests = groupBookingService.getRequestsByStatus(GroupBookingStatus.PENDING).toList()
        return ResponseEntity.ok(requests.map { GroupBookingRequestDto.from(it) })
    }
    
    @GetMapping("/assigned-to-me")
    suspend fun getMyAssignedRequests(
        @RequestHeader("X-Admin-Id") adminId: String
    ): ResponseEntity<List<GroupBookingRequestDto>> {
        val requests = groupBookingService.getAssignedRequests(adminId).toList()
        return ResponseEntity.ok(requests.map { GroupBookingRequestDto.from(it) })
    }
    
    @PostMapping("/{id}/assign")
    suspend fun assignRequest(
        @PathVariable id: String,
        @RequestBody request: AssignRequestDto
    ): ResponseEntity<GroupBookingRequestDto> {
        val groupRequest = groupBookingService.assignRequest(id, request.assignedTo)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(GroupBookingRequestDto.from(groupRequest))
    }
    
    @PostMapping("/{id}/quote")
    suspend fun submitQuote(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: SubmitQuoteRequest
    ): ResponseEntity<GroupBookingRequestDto> {
        val groupRequest = groupBookingService.submitQuote(
            requestId = id,
            quotedAmount = request.quotedAmount,
            quotedCurrency = request.quotedCurrency,
            quotedBy = adminId,
            validUntil = request.validUntil
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(GroupBookingRequestDto.from(groupRequest))
    }
    
    @PostMapping("/{id}/status")
    suspend fun updateStatus(
        @PathVariable id: String,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<GroupBookingRequestDto> {
        val status = try { GroupBookingStatus.valueOf(request.status) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val groupRequest = groupBookingService.updateStatus(id, status, request.notes, request.bookingPnr)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(GroupBookingRequestDto.from(groupRequest))
    }
}

data class CreateGroupBookingRequest(
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val origin: String,
    val destination: String,
    val departureDate: LocalDate,
    val passengerCount: Int,
    val tripType: String,
    val agencyId: String? = null,
    val companyName: String? = null,
    val returnDate: LocalDate? = null,
    val fareClassPreference: String? = null,
    val specialRequirements: String? = null
)

data class GroupBookingRequestDto(
    val id: String,
    val requestNumber: String,
    val agencyId: String?,
    val contactName: String,
    val contactEmail: String,
    val origin: String,
    val destination: String,
    val departureDate: LocalDate,
    val returnDate: LocalDate?,
    val passengerCount: Int,
    val tripType: String,
    val status: String,
    val quotedAmount: BigDecimal?,
    val quotedCurrency: String?,
    val quoteValidUntil: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: GroupBookingRequestEntity) = GroupBookingRequestDto(
            id = entity.id,
            requestNumber = entity.requestNumber,
            agencyId = entity.agencyId,
            contactName = entity.contactName,
            contactEmail = entity.contactEmail,
            origin = entity.origin,
            destination = entity.destination,
            departureDate = entity.departureDate,
            returnDate = entity.returnDate,
            passengerCount = entity.passengerCount,
            tripType = entity.tripType,
            status = entity.status,
            quotedAmount = entity.quotedAmount,
            quotedCurrency = entity.quotedCurrency,
            quoteValidUntil = entity.quoteValidUntil,
            createdAt = entity.createdAt
        )
    }
}

data class AssignRequestDto(val assignedTo: String)
data class SubmitQuoteRequest(
    val quotedAmount: BigDecimal,
    val quotedCurrency: String,
    val validUntil: Instant
)
data class UpdateStatusRequest(
    val status: String,
    val notes: String? = null,
    val bookingPnr: String? = null
)

// ============================================================================
// CHARTER REQUEST CONTROLLER
// ============================================================================

@RestController
@RequestMapping("/api/charter")
class CharterController(
    private val charterService: CharterRequestService
) {
    // Public endpoint for submitting charter requests
    @PostMapping
    suspend fun createRequest(@RequestBody request: CreateCharterRequest): ResponseEntity<CharterRequestDto> {
        return try {
            val charterRequest = charterService.createRequest(
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                charterType = CharterType.valueOf(request.charterType),
                origin = request.origin,
                destination = request.destination,
                departureDate = request.departureDate,
                passengerCount = request.passengerCount,
                agencyId = request.agencyId,
                companyName = request.companyName,
                returnDate = request.returnDate,
                aircraftPreference = request.aircraftPreference,
                cateringRequirements = request.cateringRequirements,
                specialRequirements = request.specialRequirements
            )
            ResponseEntity.status(HttpStatus.CREATED).body(CharterRequestDto.from(charterRequest))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/{requestNumber}")
    suspend fun getRequest(@PathVariable requestNumber: String): ResponseEntity<CharterRequestDto> {
        val request = charterService.getRequestByNumber(requestNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CharterRequestDto.from(request))
    }
}

@RestController
@RequestMapping("/api/admin/charter")
class AdminCharterController(
    private val charterService: CharterRequestService
) {
    @GetMapping
    suspend fun getAllRequests(): ResponseEntity<List<CharterRequestDto>> {
        val requests = charterService.getAllRequests().toList()
        return ResponseEntity.ok(requests.map { CharterRequestDto.from(it) })
    }
    
    @GetMapping("/pending")
    suspend fun getPendingRequests(): ResponseEntity<List<CharterRequestDto>> {
        val requests = charterService.getRequestsByStatus(GroupBookingStatus.PENDING).toList()
        return ResponseEntity.ok(requests.map { CharterRequestDto.from(it) })
    }
    
    @GetMapping("/by-type/{type}")
    suspend fun getRequestsByType(@PathVariable type: String): ResponseEntity<List<CharterRequestDto>> {
        val charterType = try { CharterType.valueOf(type) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val requests = charterService.getRequestsByType(charterType).toList()
        return ResponseEntity.ok(requests.map { CharterRequestDto.from(it) })
    }
    
    @PostMapping("/{id}/assign")
    suspend fun assignRequest(
        @PathVariable id: String,
        @RequestBody request: AssignRequestDto
    ): ResponseEntity<CharterRequestDto> {
        val charterRequest = charterService.assignRequest(id, request.assignedTo)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CharterRequestDto.from(charterRequest))
    }
    
    @PostMapping("/{id}/quote")
    suspend fun submitQuote(
        @PathVariable id: String,
        @RequestHeader("X-Admin-Id") adminId: String,
        @RequestBody request: SubmitQuoteRequest
    ): ResponseEntity<CharterRequestDto> {
        val charterRequest = charterService.submitQuote(
            requestId = id,
            quotedAmount = request.quotedAmount,
            quotedCurrency = request.quotedCurrency,
            quotedBy = adminId,
            validUntil = request.validUntil
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CharterRequestDto.from(charterRequest))
    }
    
    @PostMapping("/{id}/status")
    suspend fun updateStatus(
        @PathVariable id: String,
        @RequestBody request: UpdateCharterStatusRequest
    ): ResponseEntity<CharterRequestDto> {
        val status = try { GroupBookingStatus.valueOf(request.status) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val charterRequest = charterService.updateStatus(id, status, request.notes)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CharterRequestDto.from(charterRequest))
    }
}

data class CreateCharterRequest(
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String,
    val charterType: String,
    val origin: String,
    val destination: String,
    val departureDate: LocalDate,
    val passengerCount: Int,
    val agencyId: String? = null,
    val companyName: String? = null,
    val returnDate: LocalDate? = null,
    val aircraftPreference: String? = null,
    val cateringRequirements: String? = null,
    val specialRequirements: String? = null
)

data class CharterRequestDto(
    val id: String,
    val requestNumber: String,
    val agencyId: String?,
    val contactName: String,
    val contactEmail: String,
    val charterType: String,
    val origin: String,
    val destination: String,
    val departureDate: LocalDate,
    val returnDate: LocalDate?,
    val passengerCount: Int,
    val status: String,
    val quotedAmount: BigDecimal?,
    val quotedCurrency: String?,
    val quoteValidUntil: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: CharterRequestEntity) = CharterRequestDto(
            id = entity.id,
            requestNumber = entity.requestNumber,
            agencyId = entity.agencyId,
            contactName = entity.contactName,
            contactEmail = entity.contactEmail,
            charterType = entity.charterType,
            origin = entity.origin,
            destination = entity.destination,
            departureDate = entity.departureDate,
            returnDate = entity.returnDate,
            passengerCount = entity.passengerCount,
            status = entity.status,
            quotedAmount = entity.quotedAmount,
            quotedCurrency = entity.quotedCurrency,
            quoteValidUntil = entity.quoteValidUntil,
            createdAt = entity.createdAt
        )
    }
}

data class UpdateCharterStatusRequest(
    val status: String,
    val notes: String? = null
)

// ============================================================================
// B2B GROUP BOOKING & CHARTER (for agencies)
// ============================================================================

@RestController
@RequestMapping("/api/b2b/group-bookings")
class B2BGroupBookingController(
    private val groupBookingService: GroupBookingService
) {
    @GetMapping
    suspend fun getAgencyRequests(
        @RequestHeader("X-Agency-Id") agencyId: String
    ): ResponseEntity<List<GroupBookingRequestDto>> {
        val requests = groupBookingService.getAgencyRequests(agencyId).toList()
        return ResponseEntity.ok(requests.map { GroupBookingRequestDto.from(it) })
    }
    
    @PostMapping
    suspend fun createRequest(
        @RequestHeader("X-Agency-Id") agencyId: String,
        @RequestBody request: CreateGroupBookingRequest
    ): ResponseEntity<GroupBookingRequestDto> {
        return try {
            val groupRequest = groupBookingService.createRequest(
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                origin = request.origin,
                destination = request.destination,
                departureDate = request.departureDate,
                passengerCount = request.passengerCount,
                tripType = request.tripType,
                agencyId = agencyId,
                companyName = request.companyName,
                returnDate = request.returnDate,
                fareClassPreference = request.fareClassPreference,
                specialRequirements = request.specialRequirements
            )
            ResponseEntity.status(HttpStatus.CREATED).body(GroupBookingRequestDto.from(groupRequest))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}

@RestController
@RequestMapping("/api/b2b/charter")
class B2BCharterController(
    private val charterService: CharterRequestService
) {
    @GetMapping
    suspend fun getAgencyRequests(
        @RequestHeader("X-Agency-Id") agencyId: String
    ): ResponseEntity<List<CharterRequestDto>> {
        val requests = charterService.getAgencyRequests(agencyId).toList()
        return ResponseEntity.ok(requests.map { CharterRequestDto.from(it) })
    }
    
    @PostMapping
    suspend fun createRequest(
        @RequestHeader("X-Agency-Id") agencyId: String,
        @RequestBody request: CreateCharterRequest
    ): ResponseEntity<CharterRequestDto> {
        return try {
            val charterRequest = charterService.createRequest(
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                charterType = CharterType.valueOf(request.charterType),
                origin = request.origin,
                destination = request.destination,
                departureDate = request.departureDate,
                passengerCount = request.passengerCount,
                agencyId = agencyId,
                companyName = request.companyName,
                returnDate = request.returnDate,
                aircraftPreference = request.aircraftPreference,
                cateringRequirements = request.cateringRequirements,
                specialRequirements = request.specialRequirements
            )
            ResponseEntity.status(HttpStatus.CREATED).body(CharterRequestDto.from(charterRequest))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}
