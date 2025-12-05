package com.fairair.app.admin.state

import com.fairair.app.admin.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized state management for the Admin portal.
 */
class AdminState {
    // ============================================================================
    // AUTHENTICATION STATE
    // ============================================================================
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentAdmin = MutableStateFlow<AdminDto?>(null)
    val currentAdmin: StateFlow<AdminDto?> = _currentAdmin.asStateFlow()
    
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    fun setLoggedIn(admin: AdminDto, token: String) {
        _currentAdmin.value = admin
        _authToken.value = token
        _isLoggedIn.value = true
    }
    
    fun logout() {
        _currentAdmin.value = null
        _authToken.value = null
        _isLoggedIn.value = false
    }
    
    // ============================================================================
    // ADMIN USERS STATE
    // ============================================================================
    
    private val _adminUsers = MutableStateFlow<List<AdminDto>>(emptyList())
    val adminUsers: StateFlow<List<AdminDto>> = _adminUsers.asStateFlow()
    
    private val _adminUsersLoading = MutableStateFlow(false)
    val adminUsersLoading: StateFlow<Boolean> = _adminUsersLoading.asStateFlow()
    
    fun setAdminUsers(users: List<AdminDto>) {
        _adminUsers.value = users
    }
    
    fun setAdminUsersLoading(loading: Boolean) {
        _adminUsersLoading.value = loading
    }
    
    // ============================================================================
    // STATIC PAGES STATE
    // ============================================================================
    
    private val _staticPages = MutableStateFlow<List<StaticPageDto>>(emptyList())
    val staticPages: StateFlow<List<StaticPageDto>> = _staticPages.asStateFlow()
    
    private val _staticPagesLoading = MutableStateFlow(false)
    val staticPagesLoading: StateFlow<Boolean> = _staticPagesLoading.asStateFlow()
    
    private val _selectedPage = MutableStateFlow<StaticPageDto?>(null)
    val selectedPage: StateFlow<StaticPageDto?> = _selectedPage.asStateFlow()
    
    fun setStaticPages(pages: List<StaticPageDto>) {
        _staticPages.value = pages
    }
    
    fun setStaticPagesLoading(loading: Boolean) {
        _staticPagesLoading.value = loading
    }
    
    fun selectPage(page: StaticPageDto?) {
        _selectedPage.value = page
    }
    
    // ============================================================================
    // LEGAL DOCUMENTS STATE
    // ============================================================================
    
    private val _legalDocuments = MutableStateFlow<List<LegalDocumentDto>>(emptyList())
    val legalDocuments: StateFlow<List<LegalDocumentDto>> = _legalDocuments.asStateFlow()
    
    private val _legalDocumentsLoading = MutableStateFlow(false)
    val legalDocumentsLoading: StateFlow<Boolean> = _legalDocumentsLoading.asStateFlow()
    
    fun setLegalDocuments(documents: List<LegalDocumentDto>) {
        _legalDocuments.value = documents
    }
    
    fun setLegalDocumentsLoading(loading: Boolean) {
        _legalDocumentsLoading.value = loading
    }
    
    // ============================================================================
    // PROMOTIONS STATE
    // ============================================================================
    
    private val _promotions = MutableStateFlow<List<PromotionDto>>(emptyList())
    val promotions: StateFlow<List<PromotionDto>> = _promotions.asStateFlow()
    
    private val _promotionsLoading = MutableStateFlow(false)
    val promotionsLoading: StateFlow<Boolean> = _promotionsLoading.asStateFlow()
    
    private val _selectedPromotion = MutableStateFlow<PromotionDto?>(null)
    val selectedPromotion: StateFlow<PromotionDto?> = _selectedPromotion.asStateFlow()
    
    fun setPromotions(promos: List<PromotionDto>) {
        _promotions.value = promos
    }
    
    fun setPromotionsLoading(loading: Boolean) {
        _promotionsLoading.value = loading
    }
    
    fun selectPromotion(promo: PromotionDto?) {
        _selectedPromotion.value = promo
    }
    
    // ============================================================================
    // DESTINATIONS STATE
    // ============================================================================
    
    private val _destinations = MutableStateFlow<List<DestinationContentDto>>(emptyList())
    val destinations: StateFlow<List<DestinationContentDto>> = _destinations.asStateFlow()
    
    private val _destinationsLoading = MutableStateFlow(false)
    val destinationsLoading: StateFlow<Boolean> = _destinationsLoading.asStateFlow()
    
    fun setDestinations(dests: List<DestinationContentDto>) {
        _destinations.value = dests
    }
    
    fun setDestinationsLoading(loading: Boolean) {
        _destinationsLoading.value = loading
    }
    
    // ============================================================================
    // B2B - AGENCIES STATE
    // ============================================================================
    
    private val _agencies = MutableStateFlow<List<AgencyDto>>(emptyList())
    val agencies: StateFlow<List<AgencyDto>> = _agencies.asStateFlow()
    
    private val _agenciesLoading = MutableStateFlow(false)
    val agenciesLoading: StateFlow<Boolean> = _agenciesLoading.asStateFlow()
    
    private val _pendingAgencies = MutableStateFlow<List<AgencyDto>>(emptyList())
    val pendingAgencies: StateFlow<List<AgencyDto>> = _pendingAgencies.asStateFlow()
    
    fun setAgencies(list: List<AgencyDto>) {
        _agencies.value = list
    }
    
    fun setPendingAgencies(list: List<AgencyDto>) {
        _pendingAgencies.value = list
    }
    
    fun setAgenciesLoading(loading: Boolean) {
        _agenciesLoading.value = loading
    }
    
    // ============================================================================
    // B2B - GROUP BOOKINGS STATE
    // ============================================================================
    
    private val _groupBookings = MutableStateFlow<List<GroupBookingRequestDto>>(emptyList())
    val groupBookings: StateFlow<List<GroupBookingRequestDto>> = _groupBookings.asStateFlow()
    
    private val _groupBookingsLoading = MutableStateFlow(false)
    val groupBookingsLoading: StateFlow<Boolean> = _groupBookingsLoading.asStateFlow()
    
    fun setGroupBookings(list: List<GroupBookingRequestDto>) {
        _groupBookings.value = list
    }
    
    fun setGroupBookingsLoading(loading: Boolean) {
        _groupBookingsLoading.value = loading
    }
    
    // ============================================================================
    // B2B - CHARTER REQUESTS STATE
    // ============================================================================
    
    private val _charterRequests = MutableStateFlow<List<CharterRequestDto>>(emptyList())
    val charterRequests: StateFlow<List<CharterRequestDto>> = _charterRequests.asStateFlow()
    
    private val _charterRequestsLoading = MutableStateFlow(false)
    val charterRequestsLoading: StateFlow<Boolean> = _charterRequestsLoading.asStateFlow()
    
    fun setCharterRequests(list: List<CharterRequestDto>) {
        _charterRequests.value = list
    }
    
    fun setCharterRequestsLoading(loading: Boolean) {
        _charterRequestsLoading.value = loading
    }
    
    // ============================================================================
    // ERROR STATE
    // ============================================================================
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun setError(message: String?) {
        _error.value = message
    }
    
    fun clearError() {
        _error.value = null
    }
}

/**
 * Currently active section in the admin dashboard.
 */
enum class AdminSection {
    DASHBOARD,
    STATIC_PAGES,
    LEGAL_DOCUMENTS,
    PROMOTIONS,
    DESTINATIONS,
    ADMIN_USERS,
    AGENCIES,
    GROUP_BOOKINGS,
    CHARTER_REQUESTS,
    SETTINGS
}
