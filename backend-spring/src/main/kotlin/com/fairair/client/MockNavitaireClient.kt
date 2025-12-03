package com.fairair.client

import com.fairair.config.FairairProperties
import com.fairair.contract.model.*
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Mock implementation of NavitaireClient for development and testing.
 * Generates realistic flight data with configurable delays to simulate network latency.
 *
 * Activated when fairair.provider=mock (default).
 */
@Service
@ConditionalOnProperty(
    name = ["fairair.provider"],
    havingValue = "mock",
    matchIfMissing = true
)
class MockNavitaireClient(
    private val config: FairairProperties
) : NavitaireClient {

    private val log = LoggerFactory.getLogger(MockNavitaireClient::class.java)

    /**
     * In-memory store for created bookings (keyed by PNR).
     */
    private val bookingStore = ConcurrentHashMap<String, BookingConfirmation>()
    
    /**
     * Maps user IDs to their booking PNRs.
     */
    private val userBookings = ConcurrentHashMap<String, MutableList<String>>()

    /**
     * Simulates network delay based on configuration.
     */
    private suspend fun simulateDelay() {
        val delayMs = Random.nextLong(config.mock.minDelay, config.mock.maxDelay)
        log.debug("Simulating ${delayMs}ms network delay")
        delay(delayMs)
    }

    override suspend fun getRouteMap(): RouteMap {
        simulateDelay()
        log.info("Fetching mock route map")

        return RouteMap(
            routes = mapOf(
                AirportCode("JED") to listOf(
                    AirportCode("RUH"),
                    AirportCode("DMM"),
                    AirportCode("AHB"),
                    AirportCode("GIZ"),
                    AirportCode("TUU"),
                    AirportCode("DXB"),
                    AirportCode("CAI")
                ),
                AirportCode("RUH") to listOf(
                    AirportCode("JED"),
                    AirportCode("DMM"),
                    AirportCode("AHB"),
                    AirportCode("GIZ"),
                    AirportCode("TUU"),
                    AirportCode("DXB"),
                    AirportCode("CAI")
                ),
                AirportCode("DMM") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH"),
                    AirportCode("DXB")
                ),
                AirportCode("AHB") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("GIZ") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("TUU") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("DXB") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH"),
                    AirportCode("DMM")
                ),
                AirportCode("CAI") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                )
            )
        )
    }

    override suspend fun getStations(): List<Station> {
        simulateDelay()
        log.info("Fetching mock stations")

        return listOf(
            Station(AirportCode("JED"), "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia"),
            Station(AirportCode("RUH"), "King Khalid International Airport", "Riyadh", "Saudi Arabia"),
            Station(AirportCode("DMM"), "King Fahd International Airport", "Dammam", "Saudi Arabia"),
            Station(AirportCode("AHB"), "Abha International Airport", "Abha", "Saudi Arabia"),
            Station(AirportCode("GIZ"), "King Abdullah bin Abdulaziz Airport", "Jazan", "Saudi Arabia"),
            Station(AirportCode("TUU"), "Tabuk Regional Airport", "Tabuk", "Saudi Arabia"),
            Station(AirportCode("DXB"), "Dubai International Airport", "Dubai", "United Arab Emirates"),
            Station(AirportCode("CAI"), "Cairo International Airport", "Cairo", "Egypt")
        )
    }

    override suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        simulateDelay()
        log.info("Searching flights: ${request.origin} -> ${request.destination} on ${request.departureDate}")

        val searchId = UUID.randomUUID().toString()
        val flights = generateMockFlights(request)

        return FlightResponse(
            flights = flights,
            searchId = searchId
        )
    }

    override suspend fun createBooking(request: BookingRequest, userId: String?): BookingConfirmation {
        simulateDelay()
        log.info("Creating booking for flight ${request.flightNumber} with ${request.passengers.size} passengers, userId=$userId")

        val pnr = PnrCode.generate()
        val now = Clock.System.now()

        val confirmation = BookingConfirmation(
            pnr = pnr,
            bookingReference = UUID.randomUUID().toString(),
            flight = FlightSummary(
                flightNumber = request.flightNumber,
                origin = AirportCode("JED"),
                destination = AirportCode("RUH"),
                departureTime = now.plus(1, DateTimeUnit.DAY, TimeZone.UTC),
                fareFamily = request.fareFamily
            ),
            passengers = request.passengers.map { passenger ->
                PassengerSummary(
                    fullName = passenger.fullName,
                    type = passenger.type
                )
            },
            totalPaid = request.payment.totalAmount,
            createdAt = now
        )

        bookingStore[pnr.value] = confirmation
        
        // Associate booking with user if logged in
        if (userId != null) {
            userBookings.getOrPut(userId) { mutableListOf() }.add(pnr.value)
            log.info("Booking ${pnr.value} associated with user $userId")
        }
        
        log.info("Booking created with PNR=${pnr.value}")

        return confirmation
    }

    override suspend fun getBooking(pnr: String): BookingConfirmation? {
        simulateDelay()
        log.info("Retrieving booking for PNR=$pnr")
        return bookingStore[pnr]
    }
    
    override suspend fun getBookingsByUser(userId: String): List<BookingConfirmation> {
        simulateDelay()
        log.info("Retrieving bookings for user=$userId")
        val pnrs = userBookings[userId] ?: return emptyList()
        return pnrs.mapNotNull { bookingStore[it] }
    }

    /**
     * Generates mock flight data for the given search request.
     */
    private fun generateMockFlights(request: FlightSearchRequest): List<Flight> {
        val baseDate = request.departureDate.atStartOfDayIn(TimeZone.of("Asia/Riyadh"))
        val flightCount = Random.nextInt(3, 7)

        return (0 until flightCount).map { index ->
            val departureHour = 6 + (index * 3)
            val departureTime = baseDate.plus(departureHour, DateTimeUnit.HOUR)
            val durationMinutes = getDurationForRoute(request.origin, request.destination)
            val arrivalTime = departureTime.plus(durationMinutes, DateTimeUnit.MINUTE)

            val basePrice = getBasePriceForRoute(request.origin, request.destination)

            Flight(
                flightNumber = "F3${100 + index}",
                origin = request.origin,
                destination = request.destination,
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                durationMinutes = durationMinutes,
                aircraft = if (index % 2 == 0) "A320neo" else "A321neo",
                fareFamilies = listOf(
                    FareFamily(
                        code = FareFamilyCode.FLY,
                        name = "Fly",
                        price = Money.sar(basePrice * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_INCLUSIONS
                    ),
                    FareFamily(
                        code = FareFamilyCode.FLY_PLUS,
                        name = "Fly+",
                        price = Money.sar(basePrice * 1.3 * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_PLUS_INCLUSIONS
                    ),
                    FareFamily(
                        code = FareFamilyCode.FLY_MAX,
                        name = "FlyMax",
                        price = Money.sar(basePrice * 1.8 * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_MAX_INCLUSIONS
                    )
                )
            )
        }
    }

    /**
     * Returns estimated flight duration in minutes for a route.
     */
    private fun getDurationForRoute(origin: AirportCode, destination: AirportCode): Int {
        return when {
            setOf(origin.value, destination.value) == setOf("JED", "RUH") -> 85
            setOf(origin.value, destination.value) == setOf("JED", "DMM") -> 120
            setOf(origin.value, destination.value) == setOf("RUH", "DMM") -> 65
            setOf(origin.value, destination.value).contains("DXB") -> 150
            setOf(origin.value, destination.value).contains("CAI") -> 180
            else -> 90
        }
    }

    /**
     * Returns base price in SAR for a route (per adult).
     */
    private fun getBasePriceForRoute(origin: AirportCode, destination: AirportCode): Double {
        val randomFactor = Random.nextDouble(0.9, 1.1)
        val basePrice = when {
            setOf(origin.value, destination.value) == setOf("JED", "RUH") -> 350.0
            setOf(origin.value, destination.value) == setOf("JED", "DMM") -> 450.0
            setOf(origin.value, destination.value) == setOf("RUH", "DMM") -> 280.0
            setOf(origin.value, destination.value).contains("DXB") -> 650.0
            setOf(origin.value, destination.value).contains("CAI") -> 850.0
            else -> 400.0
        }
        return basePrice * randomFactor
    }
}
