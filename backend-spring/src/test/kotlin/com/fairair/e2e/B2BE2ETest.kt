package com.fairair.e2e

import com.fairair.controller.*
import com.fairair.entity.*
import com.fairair.repository.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class B2BE2ETest {

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository

    @Autowired
    lateinit var agencyRepository: AgencyRepository

    @Autowired
    lateinit var agencyUserRepository: AgencyUserRepository

    @Autowired
    lateinit var groupBookingRequestRepository: GroupBookingRequestRepository

    @Autowired
    lateinit var charterRequestRepository: CharterRequestRepository

    @Autowired
    lateinit var staticPageRepository: StaticPageRepository

    @Autowired
    lateinit var legalDocumentRepository: LegalDocumentRepository

    @Autowired
    lateinit var promotionRepository: PromotionRepository

    @Autowired
    lateinit var destinationContentRepository: DestinationContentRepository

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var testAdminId: String
    private lateinit var testAgencyId: String
    private lateinit var testAgentUserId: String

    @BeforeAll
    fun setup() = runBlocking {
        // Clean up any existing test data first
        // Delete child tables before parent tables (respect foreign key constraints)
        charterRequestRepository.deleteAll()
        groupBookingRequestRepository.deleteAll()
        agencyUserRepository.deleteAll()
        agencyRepository.deleteAll()
        // Also clean admin data in case it was left from other tests
        destinationContentRepository.deleteAll()
        promotionRepository.deleteAll()
        legalDocumentRepository.deleteAll()
        staticPageRepository.deleteAll()
        adminUserRepository.deleteAll()
        
        // Create test admin user for B2B sales
        val admin = AdminUserEntity(
            id = UUID.randomUUID().toString(),
            email = "b2b-admin@test.com",
            passwordHash = passwordEncoder.encode("password123"),
            firstName = "B2B",
            lastName = "Admin",
            role = AdminRole.B2B_SALES.name,
            department = "Sales",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(admin).awaitSingle()
        testAdminId = admin.id

        // Create test agency
        val agency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "AGN001",
            name = "Test Travel Agency",
            type = AgencyType.TRAVEL_AGENT.name,
            contactName = "John Agent",
            contactEmail = "agent@testagency.com",
            status = AgencyStatus.APPROVED.name,
            commissionRate = BigDecimal("5.0"),
            creditLimit = BigDecimal("10000"),
            currentBalance = BigDecimal.ZERO,
            approvedBy = testAdminId,
            approvedAt = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(agency).awaitSingle()
        testAgencyId = agency.id

        // Create test agency user
        val agencyUser = AgencyUserEntity(
            id = UUID.randomUUID().toString(),
            agencyId = testAgencyId,
            email = "user@testagency.com",
            passwordHash = passwordEncoder.encode("agentpass"),
            firstName = "Agent",
            lastName = "User",
            role = AgencyUserRole.AGENT.name,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(agencyUser).awaitSingle()
        testAgentUserId = agencyUser.id
    }

    @AfterAll
    fun cleanup() = runBlocking {
        // Delete child tables before parent tables
        charterRequestRepository.deleteAll()
        groupBookingRequestRepository.deleteAll()
        agencyUserRepository.deleteAll()
        agencyRepository.deleteAll()
        destinationContentRepository.deleteAll()
        promotionRepository.deleteAll()
        legalDocumentRepository.deleteAll()
        staticPageRepository.deleteAll()
        adminUserRepository.deleteAll()
    }

    // ============================================================================
    // AGENCY REGISTRATION TESTS
    // ============================================================================

    @Test
    fun `register new agency`() {
        webClient.post()
            .uri("/api/b2b/auth/register-agency")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "name": "New Travel Co",
                    "type": "TRAVEL_AGENT",
                    "contactName": "Jane Doe",
                    "contactEmail": "jane@newtravel.com",
                    "contactPhone": "+966501234567",
                    "city": "Riyadh",
                    "country": "Saudi Arabia",
                    "licenseNumber": "LIC123456"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.agency.name").isEqualTo("New Travel Co")
            .jsonPath("$.agency.status").isEqualTo("PENDING")
    }

    @Test
    fun `duplicate agency email registration fails`() = runBlocking {
        // Create agency with specific email
        val agency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "AGN002",
            name = "Existing Agency",
            type = AgencyType.TRAVEL_AGENT.name,
            contactName = "Existing",
            contactEmail = "existing@agency.com",
            status = AgencyStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(agency).awaitSingle()

        webClient.post()
            .uri("/api/b2b/auth/register-agency")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "name": "Duplicate Agency",
                    "type": "TRAVEL_AGENT",
                    "contactName": "Test",
                    "contactEmail": "existing@agency.com"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isBadRequest
    }

    // ============================================================================
    // B2B LOGIN TESTS
    // ============================================================================

    @Test
    fun `agency user login succeeds`() {
        webClient.post()
            .uri("/api/b2b/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "user@testagency.com", "password": "agentpass"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.user.email").isEqualTo("user@testagency.com")
            .jsonPath("$.agency.agencyCode").isEqualTo("AGN001")
            .jsonPath("$.token").exists()
    }

    @Test
    fun `agency user login fails with wrong password`() {
        webClient.post()
            .uri("/api/b2b/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "user@testagency.com", "password": "wrongpassword"}""")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `agency user from pending agency cannot login`() = runBlocking {
        // Create pending agency
        val pendingAgency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "PEND01",
            name = "Pending Agency",
            type = AgencyType.TRAVEL_AGENT.name,
            contactName = "Pending",
            contactEmail = "pending@test.com",
            status = AgencyStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(pendingAgency).awaitSingle()

        // Create user for pending agency
        val user = AgencyUserEntity(
            id = UUID.randomUUID().toString(),
            agencyId = pendingAgency.id,
            email = "pendinguser@test.com",
            passwordHash = passwordEncoder.encode("password"),
            firstName = "Pending",
            lastName = "User",
            role = AgencyUserRole.AGENT.name,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(user).awaitSingle()

        webClient.post()
            .uri("/api/b2b/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "pendinguser@test.com", "password": "password"}""")
            .exchange()
            .expectStatus().isUnauthorized
    }

    // ============================================================================
    // AGENCY MANAGEMENT TESTS (Admin)
    // ============================================================================

    @Test
    fun `get all agencies`() {
        webClient.get()
            .uri("/api/admin/agencies")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `get pending agencies`() = runBlocking {
        // Create pending agency
        val pendingAgency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "PEND02",
            name = "Pending Agency 2",
            type = AgencyType.CORPORATE.name,
            contactName = "Corporate",
            contactEmail = "corporate@test.com",
            status = AgencyStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(pendingAgency).awaitSingle()

        webClient.get()
            .uri("/api/admin/agencies/pending")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `approve agency`() = runBlocking {
        // Create agency to approve
        val agency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "APRV01",
            name = "Agency to Approve",
            type = AgencyType.TRAVEL_AGENT.name,
            contactName = "To Approve",
            contactEmail = "toapprove@test.com",
            status = AgencyStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(agency).awaitSingle()

        webClient.post()
            .uri("/api/admin/agencies/${agency.id}/approve")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""{"commissionRate": 7.5, "creditLimit": 5000}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("APPROVED")
            .jsonPath("$.commissionRate").isEqualTo(7.5)
            .jsonPath("$.creditLimit").isEqualTo(5000)
    }

    @Test
    fun `suspend agency`() = runBlocking {
        // Create approved agency
        val agency = AgencyEntity(
            id = UUID.randomUUID().toString(),
            agencyCode = "SUSP01",
            name = "Agency to Suspend",
            type = AgencyType.TRAVEL_AGENT.name,
            contactName = "To Suspend",
            contactEmail = "tosuspend@test.com",
            status = AgencyStatus.APPROVED.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(agency).awaitSingle()

        webClient.post()
            .uri("/api/admin/agencies/${agency.id}/suspend")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"notes": "Violated terms of service"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("SUSPENDED")
    }

    // ============================================================================
    // B2B PORTAL TESTS
    // ============================================================================

    @Test
    fun `get my agency info`() {
        webClient.get()
            .uri("/api/b2b/portal/agency")
            .header("X-Agency-Id", testAgencyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.agencyCode").isEqualTo("AGN001")
            .jsonPath("$.name").isEqualTo("Test Travel Agency")
    }

    @Test
    fun `create agency user`() {
        webClient.post()
            .uri("/api/b2b/portal/users")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Agency-Id", testAgencyId)
            .bodyValue("""
                {
                    "email": "newagent@testagency.com",
                    "password": "newagentpass",
                    "firstName": "New",
                    "lastName": "Agent",
                    "role": "AGENT"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.email").isEqualTo("newagent@testagency.com")
            .jsonPath("$.role").isEqualTo("AGENT")
    }

    // ============================================================================
    // GROUP BOOKING TESTS
    // ============================================================================

    @Test
    fun `create group booking request (public)`() {
        webClient.post()
            .uri("/api/group-bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "contactName": "Group Leader",
                    "contactEmail": "leader@group.com",
                    "contactPhone": "+966501234567",
                    "origin": "RUH",
                    "destination": "JED",
                    "departureDate": "${LocalDate.now().plusDays(30)}",
                    "passengerCount": 25,
                    "tripType": "ROUND_TRIP",
                    "companyName": "Test Corporation"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.requestNumber").exists()
            .jsonPath("$.passengerCount").isEqualTo(25)
            .jsonPath("$.status").isEqualTo("PENDING")
    }

    @Test
    fun `group booking request requires minimum 10 passengers`() {
        webClient.post()
            .uri("/api/group-bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "contactName": "Small Group",
                    "contactEmail": "small@group.com",
                    "contactPhone": "+966501234567",
                    "origin": "RUH",
                    "destination": "JED",
                    "departureDate": "${LocalDate.now().plusDays(30)}",
                    "passengerCount": 5,
                    "tripType": "ONE_WAY"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `create group booking via B2B portal`() {
        webClient.post()
            .uri("/api/b2b/group-bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Agency-Id", testAgencyId)
            .bodyValue("""
                {
                    "contactName": "Agency Client",
                    "contactEmail": "client@agency.com",
                    "contactPhone": "+966501234567",
                    "origin": "RUH",
                    "destination": "DXB",
                    "departureDate": "${LocalDate.now().plusDays(45)}",
                    "passengerCount": 50,
                    "tripType": "ROUND_TRIP",
                    "fareClassPreference": "ECONOMY"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.agencyId").isEqualTo(testAgencyId)
    }

    @Test
    fun `admin can assign and quote group booking`() = runBlocking {
        // Create group request
        val request = GroupBookingRequestEntity(
            id = UUID.randomUUID().toString(),
            requestNumber = "GRP-TEST-001",
            contactName = "Test Contact",
            contactEmail = "test@contact.com",
            contactPhone = "+966501234567",
            origin = "RUH",
            destination = "JED",
            departureDate = LocalDate.now().plusDays(60),
            passengerCount = 30,
            tripType = "ROUND_TRIP",
            status = GroupBookingStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(request).awaitSingle()

        // Assign
        webClient.post()
            .uri("/api/admin/group-bookings/${request.id}/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"assignedTo": "$testAdminId"}""")
            .exchange()
            .expectStatus().isOk

        // Quote
        val validUntil = Instant.now().plusSeconds(86400 * 7) // 7 days
        webClient.post()
            .uri("/api/admin/group-bookings/${request.id}/quote")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "quotedAmount": 45000,
                    "quotedCurrency": "SAR",
                    "validUntil": "$validUntil"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("QUOTED")
            .jsonPath("$.quotedAmount").isEqualTo(45000)
    }

    // ============================================================================
    // CHARTER REQUEST TESTS
    // ============================================================================

    @Test
    fun `create charter request (public)`() {
        webClient.post()
            .uri("/api/charter")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "contactName": "Charter Organizer",
                    "contactEmail": "charter@company.com",
                    "contactPhone": "+966501234567",
                    "charterType": "SPORTS_TEAM",
                    "origin": "RUH",
                    "destination": "DOH",
                    "departureDate": "${LocalDate.now().plusDays(14)}",
                    "passengerCount": 45,
                    "companyName": "Saudi Football Club",
                    "aircraftPreference": "A321",
                    "cateringRequirements": "Halal meals for all passengers"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.requestNumber").exists()
            .jsonPath("$.charterType").isEqualTo("SPORTS_TEAM")
            .jsonPath("$.status").isEqualTo("PENDING")
    }

    @Test
    fun `create Hajj charter request via B2B`() {
        webClient.post()
            .uri("/api/b2b/charter")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Agency-Id", testAgencyId)
            .bodyValue("""
                {
                    "contactName": "Hajj Organizer",
                    "contactEmail": "hajj@agency.com",
                    "contactPhone": "+966501234567",
                    "charterType": "HAJJ_UMRAH",
                    "origin": "IST",
                    "destination": "JED",
                    "departureDate": "${LocalDate.now().plusDays(90)}",
                    "passengerCount": 180,
                    "specialRequirements": "Wheelchair assistance for 5 passengers"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.agencyId").isEqualTo(testAgencyId)
            .jsonPath("$.charterType").isEqualTo("HAJJ_UMRAH")
    }

    @Test
    fun `get charter requests by type`() = runBlocking {
        // Create charter requests of different types
        val sportsCharter = CharterRequestEntity(
            id = UUID.randomUUID().toString(),
            requestNumber = "CHR-SPORT-001",
            contactName = "Sports",
            contactEmail = "sports@test.com",
            contactPhone = "+966501234567",
            charterType = CharterType.SPORTS_TEAM.name,
            origin = "RUH",
            destination = "CAI",
            departureDate = LocalDate.now().plusDays(30),
            passengerCount = 50,
            status = GroupBookingStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(sportsCharter).awaitSingle()

        webClient.get()
            .uri("/api/admin/charter/by-type/SPORTS_TEAM")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `admin can quote charter request`() = runBlocking {
        // Create charter request
        val request = CharterRequestEntity(
            id = UUID.randomUUID().toString(),
            requestNumber = "CHR-QUOTE-001",
            contactName = "Quote Test",
            contactEmail = "quote@test.com",
            contactPhone = "+966501234567",
            charterType = CharterType.CORPORATE.name,
            origin = "RUH",
            destination = "DXB",
            departureDate = LocalDate.now().plusDays(45),
            passengerCount = 80,
            status = GroupBookingStatus.PENDING.name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(request).awaitSingle()

        val validUntil = Instant.now().plusSeconds(86400 * 14) // 14 days
        webClient.post()
            .uri("/api/admin/charter/${request.id}/quote")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "quotedAmount": 150000,
                    "quotedCurrency": "SAR",
                    "validUntil": "$validUntil"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("QUOTED")
            .jsonPath("$.quotedAmount").isEqualTo(150000)
    }

    // ============================================================================
    // B2B DASHBOARD TESTS
    // ============================================================================

    @Test
    fun `get agency bookings`() {
        webClient.get()
            .uri("/api/b2b/portal/bookings")
            .header("X-Agency-Id", testAgencyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `get agency group booking requests`() {
        webClient.get()
            .uri("/api/b2b/group-bookings")
            .header("X-Agency-Id", testAgencyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `get agency charter requests`() {
        webClient.get()
            .uri("/api/b2b/charter")
            .header("X-Agency-Id", testAgencyId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }
}
