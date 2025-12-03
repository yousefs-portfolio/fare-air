package com.fairair.app.viewmodel

import com.fairair.app.api.*
import com.fairair.app.state.BookingFlowState
import com.fairair.app.state.SearchCriteria
import com.fairair.app.ui.components.velocity.DestinationTheme
import com.fairair.app.ui.components.velocity.PassengerCounts
import com.fairair.app.ui.screens.search.SearchField
import com.fairair.app.ui.screens.search.VelocitySearchState
import com.fairair.app.util.toDisplayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Search screen that works without Voyager dependency.
 * This is used for Wasm target where Voyager Navigator causes rendering issues.
 *
 * Unlike SearchScreenModel which extends Voyager's ScreenModel, this class
 * takes a CoroutineScope parameter that can be provided by the composable
 * using rememberCoroutineScope().
 */
class WasmSearchViewModel(
    private val apiClient: FairairApiClient,
    private val bookingFlowState: BookingFlowState,
    private val scope: CoroutineScope
) {
    private val _velocityState = MutableStateFlow(VelocitySearchState())
    val velocityState: StateFlow<VelocitySearchState> = _velocityState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Loads stations and routes on init.
     */
    private fun loadInitialData() {
        scope.launch {
            _velocityState.update { it.copy(isLoading = true, error = null) }

            val stationsResult = apiClient.getStations()
            val routesResult = apiClient.getRoutes()

            when {
                stationsResult is ApiResult.Success && routesResult is ApiResult.Success -> {
                    _velocityState.update {
                        it.copy(
                            isLoading = false,
                            availableOrigins = stationsResult.data,
                            routeMap = routesResult.data.routes
                        )
                    }
                }
                stationsResult is ApiResult.Error -> {
                    _velocityState.update {
                        it.copy(isLoading = false, error = stationsResult.toDisplayMessage())
                    }
                }
                routesResult is ApiResult.Error -> {
                    _velocityState.update {
                        it.copy(isLoading = false, error = (routesResult as ApiResult.Error).toDisplayMessage())
                    }
                }
            }
        }
    }

    /**
     * Sets the active field for the Velocity sentence builder UI.
     * Pass null to dismiss any open selection sheet.
     */
    fun setActiveField(field: SearchField?) {
        _velocityState.update { it.copy(activeField = field) }
    }

    /**
     * Selects an origin airport (Velocity UI).
     */
    fun selectVelocityOrigin(station: StationDto) {
        _velocityState.update { state ->
            val validDestinationCodes = state.routeMap[station.code] ?: emptyList()
            val availableDestinations = state.availableOrigins.filter { it.code in validDestinationCodes }

            // If current destination is not in the new valid destinations list, clear it
            val currentDestination = state.selectedDestination
            val newDestination = if (currentDestination != null &&
                validDestinationCodes.contains(currentDestination.code)) {
                currentDestination
            } else {
                null
            }

            state.copy(
                selectedOrigin = station,
                selectedDestination = newDestination,
                availableDestinations = availableDestinations,
                // Clear destination background if destination was cleared
                destinationBackground = if (newDestination != null) state.destinationBackground else null
            )
        }
    }

    /**
     * Selects a destination airport (Velocity UI).
     */
    fun selectVelocityDestination(station: StationDto) {
        _velocityState.update { state ->
            state.copy(
                selectedDestination = station,
                destinationBackground = DestinationTheme.forDestination(station.code)
            )
        }
    }

    /**
     * Selects a departure date (Velocity UI).
     */
    fun selectVelocityDate(date: LocalDate) {
        _velocityState.update { it.copy(departureDate = date) }
    }

    /**
     * Updates passenger counts (Velocity UI).
     * Supports adults, children, and infants.
     */
    fun setVelocityPassengerCount(counts: PassengerCounts) {
        _velocityState.update {
            it.copy(
                adultsCount = counts.adults.coerceIn(1, 9),
                childrenCount = counts.children.coerceIn(0, 8),
                infantsCount = counts.infants.coerceIn(0, counts.adults.coerceAtMost(4))
            )
        }
    }

    /**
     * Initiates flight search from Velocity UI.
     */
    fun searchFromVelocity(onSearchComplete: () -> Unit) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        val date = state.departureDate ?: return

        scope.launch {
            _velocityState.update { it.copy(isSearching = true, error = null) }

            val request = FlightSearchRequestDto(
                origin = origin.code,
                destination = destination.code,
                departureDate = date.toString(),
                passengers = PassengerCountsDto(
                    adults = state.adultsCount,
                    children = state.childrenCount,
                    infants = state.infantsCount
                )
            )

            when (val result = apiClient.searchFlights(request)) {
                is ApiResult.Success -> {
                    bookingFlowState.setSearchCriteria(
                        SearchCriteria(
                            origin = origin,
                            destination = destination,
                            departureDate = date.toString(),
                            passengers = request.passengers
                        )
                    )
                    bookingFlowState.setSearchResult(result.data)
                    _velocityState.update { it.copy(isSearching = false) }
                    onSearchComplete()
                }
                is ApiResult.Error -> {
                    val errorMessage = result.toDisplayMessage()
                    _velocityState.update {
                        it.copy(isSearching = false, error = errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _velocityState.update { it.copy(error = null) }
    }

    /**
     * Retries loading initial data.
     */
    fun retry() {
        _velocityState.update { it.copy(isLoading = true, error = null) }
        loadInitialData()
    }

    /**
     * Preselects an origin by airport code.
     * If the code matches an available origin, it will be selected.
     */
    fun preselectOrigin(code: String) {
        val state = _velocityState.value
        val station = state.availableOrigins.find { it.code == code }
        if (station != null) {
            selectVelocityOrigin(station)
        }
    }

    /**
     * Preselects a destination by airport code.
     * Clears any previously selected origin.
     * If the code matches an available station, it will be selected as destination.
     */
    fun preselectDestination(code: String) {
        // Clear the origin first
        _velocityState.update { state ->
            state.copy(
                selectedOrigin = null,
                selectedDestination = null,
                availableDestinations = emptyList(),
                destinationBackground = null
            )
        }
        // Then set the destination
        val state = _velocityState.value
        val station = state.availableOrigins.find { it.code == code }
        if (station != null) {
            selectVelocityDestination(station)
        }
    }

    /**
     * Preselects both origin and destination by airport codes.
     */
    fun preselectRoute(originCode: String, destinationCode: String) {
        // First select origin, which will populate available destinations
        preselectOrigin(originCode)
        // Then select destination from the now-available destinations
        val state = _velocityState.value
        val destinationStation = state.availableDestinations.find { it.code == destinationCode }
            ?: state.availableOrigins.find { it.code == destinationCode }
        if (destinationStation != null) {
            selectVelocityDestination(destinationStation)
        }
    }
}
