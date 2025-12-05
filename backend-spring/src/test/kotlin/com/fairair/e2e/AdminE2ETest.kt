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
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminE2ETest {

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository

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

    @BeforeAll
    fun setup() = runBlocking {
        // Clean up any existing test data first
        // Delete child tables before parent tables (respect foreign key constraints)
        destinationContentRepository.deleteAll()
        promotionRepository.deleteAll()
        legalDocumentRepository.deleteAll()
        staticPageRepository.deleteAll()
        adminUserRepository.deleteAll()
        
        // Create test admin user using insert
        val admin = AdminUserEntity(
            id = UUID.randomUUID().toString(),
            email = "admin@test.com",
            passwordHash = passwordEncoder.encode("password123"),
            firstName = "Test",
            lastName = "Admin",
            role = AdminRole.SUPER_ADMIN.name,
            department = "IT",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(admin).awaitSingle()
        testAdminId = admin.id
    }

    @AfterAll
    fun cleanup() = runBlocking {
        // Delete child tables before parent tables
        destinationContentRepository.deleteAll()
        promotionRepository.deleteAll()
        legalDocumentRepository.deleteAll()
        staticPageRepository.deleteAll()
        adminUserRepository.deleteAll()
    }

    // ============================================================================
    // ADMIN AUTH TESTS
    // ============================================================================

    @Test
    fun `admin login with valid credentials succeeds`() {
        webClient.post()
            .uri("/api/admin/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "admin@test.com", "password": "password123"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.admin.email").isEqualTo("admin@test.com")
            .jsonPath("$.token").exists()
    }

    @Test
    fun `admin login with invalid credentials fails`() {
        webClient.post()
            .uri("/api/admin/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "admin@test.com", "password": "wrongpassword"}""")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
    }

    @Test
    fun `admin login with non-existent email fails`() {
        webClient.post()
            .uri("/api/admin/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "nobody@test.com", "password": "password123"}""")
            .exchange()
            .expectStatus().isUnauthorized
    }

    // ============================================================================
    // ADMIN USER MANAGEMENT TESTS
    // ============================================================================

    @Test
    fun `get all admin users returns list`() {
        webClient.get()
            .uri("/api/admin/users")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `create admin user succeeds`() {
        webClient.post()
            .uri("/api/admin/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "newadmin@test.com",
                    "password": "newpassword123",
                    "firstName": "New",
                    "lastName": "Admin",
                    "role": "CONTENT_MANAGER",
                    "department": "Marketing"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.email").isEqualTo("newadmin@test.com")
            .jsonPath("$.role").isEqualTo("CONTENT_MANAGER")
    }

    // ============================================================================
    // STATIC PAGE TESTS
    // ============================================================================

    @Test
    fun `create and retrieve static page`() {
        // Create page
        webClient.post()
            .uri("/api/admin/content/pages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "slug": "about-us",
                    "title": "About Us",
                    "content": "<p>About FairAir...</p>",
                    "titleAr": "من نحن",
                    "contentAr": "<p>عن فيرإير...</p>",
                    "metaDescription": "Learn about FairAir"
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.slug").isEqualTo("about-us")
            .jsonPath("$.title").isEqualTo("About Us")
            .jsonPath("$.isPublished").isEqualTo(false)

        // Get page by slug
        webClient.get()
            .uri("/api/admin/content/pages/about-us")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.slug").isEqualTo("about-us")
    }

    @Test
    fun `publish and unpublish static page`() = runBlocking {
        // First create a page
        val page = StaticPageEntity(
            id = UUID.randomUUID().toString(),
            slug = "test-page-publish",
            title = "Test Page",
            content = "Test content",
            isPublished = false,
            createdBy = testAdminId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(page).awaitSingle()

        // Publish
        webClient.post()
            .uri("/api/admin/content/pages/${page.id}/publish")
            .header("X-Admin-Id", testAdminId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isPublished").isEqualTo(true)

        // Unpublish
        webClient.post()
            .uri("/api/admin/content/pages/${page.id}/unpublish")
            .header("X-Admin-Id", testAdminId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isPublished").isEqualTo(false)
    }

    // ============================================================================
    // LEGAL DOCUMENT TESTS
    // ============================================================================

    @Test
    fun `create legal document`() {
        webClient.post()
            .uri("/api/admin/content/legal")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "type": "PRIVACY_POLICY",
                    "version": "1.0",
                    "title": "Privacy Policy",
                    "content": "Our privacy policy...",
                    "effectiveDate": "${LocalDate.now()}",
                    "makeCurrent": true
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.type").isEqualTo("PRIVACY_POLICY")
            .jsonPath("$.version").isEqualTo("1.0")
            .jsonPath("$.isCurrent").isEqualTo(true)
    }

    @Test
    fun `get current legal document`() = runBlocking {
        // Create a current document first
        val doc = LegalDocumentEntity(
            id = UUID.randomUUID().toString(),
            type = LegalDocumentType.TERMS_OF_USE.name,
            version = "1.0",
            title = "Terms of Use",
            content = "Terms content...",
            effectiveDate = LocalDate.now(),
            isCurrent = true,
            createdBy = testAdminId,
            createdAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(doc).awaitSingle()

        webClient.get()
            .uri("/api/admin/content/legal/TERMS_OF_USE")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.type").isEqualTo("TERMS_OF_USE")
            .jsonPath("$.isCurrent").isEqualTo(true)
    }

    // ============================================================================
    // PROMOTION TESTS
    // ============================================================================

    @Test
    fun `create promotion with code`() {
        val startDate = Instant.now()
        val endDate = startDate.plus(30, ChronoUnit.DAYS)
        
        webClient.post()
            .uri("/api/admin/promotions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "code": "SAVE20",
                    "title": "Save 20%",
                    "discountType": "PERCENTAGE",
                    "discountValue": 20.0,
                    "startDate": "$startDate",
                    "endDate": "$endDate",
                    "minPurchaseAmount": 100.0,
                    "maxDiscountAmount": 50.0
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.code").isEqualTo("SAVE20")
            .jsonPath("$.discountType").isEqualTo("PERCENTAGE")
            .jsonPath("$.discountValue").isEqualTo(20.0)
            .jsonPath("$.isActive").isEqualTo(true)
    }

    @Test
    fun `validate promotion code`() = runBlocking {
        // Create active promotion
        val promo = PromotionEntity(
            id = UUID.randomUUID().toString(),
            code = "TESTCODE",
            title = "Test Promo",
            discountType = DiscountType.PERCENTAGE.name,
            discountValue = BigDecimal("15.0"),
            startDate = Instant.now().minus(1, ChronoUnit.DAYS),
            endDate = Instant.now().plus(30, ChronoUnit.DAYS),
            isActive = true,
            createdBy = testAdminId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(promo).awaitSingle()

        webClient.get()
            .uri("/api/admin/promotions/validate/TESTCODE?amount=100")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isValid").isEqualTo(true)
            .jsonPath("$.discountAmount").isEqualTo(15.0) // 15% of 100
    }

    @Test
    fun `expired promotion validation fails`() = runBlocking {
        // Create expired promotion
        val promo = PromotionEntity(
            id = UUID.randomUUID().toString(),
            code = "EXPIRED",
            title = "Expired Promo",
            discountType = DiscountType.PERCENTAGE.name,
            discountValue = BigDecimal("10.0"),
            startDate = Instant.now().minus(60, ChronoUnit.DAYS),
            endDate = Instant.now().minus(30, ChronoUnit.DAYS),
            isActive = true,
            createdBy = testAdminId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(promo).awaitSingle()

        webClient.get()
            .uri("/api/admin/promotions/validate/EXPIRED?amount=100")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isValid").isEqualTo(false)
    }

    @Test
    fun `get active promotions`() {
        webClient.get()
            .uri("/api/admin/promotions/active")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    // ============================================================================
    // DESTINATION CONTENT TESTS
    // ============================================================================

    @Test
    fun `create destination content`() {
        webClient.post()
            .uri("/api/admin/content/destinations")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Admin-Id", testAdminId)
            .bodyValue("""
                {
                    "airportCode": "DXB",
                    "cityName": "Dubai",
                    "country": "United Arab Emirates",
                    "cityNameAr": "دبي",
                    "countryAr": "الإمارات العربية المتحدة",
                    "description": "Experience the magic of Dubai...",
                    "isFeatured": true
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.airportCode").isEqualTo("DXB")
            .jsonPath("$.cityName").isEqualTo("Dubai")
            .jsonPath("$.isFeatured").isEqualTo(true)
    }

    @Test
    fun `get featured destinations`() = runBlocking {
        // Create featured destination
        val dest = DestinationContentEntity(
            id = UUID.randomUUID().toString(),
            airportCode = "CAI",
            cityName = "Cairo",
            country = "Egypt",
            isFeatured = true,
            isPublished = true,
            createdBy = testAdminId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(dest).awaitSingle()

        webClient.get()
            .uri("/api/admin/content/destinations/featured")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    // ============================================================================
    // PUBLIC CONTENT API TESTS
    // ============================================================================

    @Test
    fun `public API returns only published pages`() = runBlocking {
        // Create unpublished page
        val unpublishedPage = StaticPageEntity(
            id = UUID.randomUUID().toString(),
            slug = "unpublished-page",
            title = "Unpublished",
            content = "Content",
            isPublished = false,
            createdBy = testAdminId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(unpublishedPage).awaitSingle()

        webClient.get()
            .uri("/api/content/pages/unpublished-page")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `public API returns current legal document`() = runBlocking {
        val doc = LegalDocumentEntity(
            id = UUID.randomUUID().toString(),
            type = LegalDocumentType.COOKIE_POLICY.name,
            version = "1.0",
            title = "Cookie Policy",
            content = "Cookie content...",
            effectiveDate = LocalDate.now(),
            isCurrent = true,
            createdBy = testAdminId,
            createdAt = Instant.now()
        )
        r2dbcEntityTemplate.insert(doc).awaitSingle()

        webClient.get()
            .uri("/api/content/legal/COOKIE_POLICY")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.type").isEqualTo("COOKIE_POLICY")
    }
}
