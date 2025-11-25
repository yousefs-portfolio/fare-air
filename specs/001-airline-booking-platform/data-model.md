# Data Model: Airline Booking Platform

**Branch**: `001-airline-booking-platform`
**Date**: 2025-11-25
**Module**: `:shared-contract` (commonMain)

---

## Overview

All entities are defined in the `:shared-contract` module using Kotlin data classes with `@Serializable` annotation. These serve as the single source of truth for both backend and frontend.

---

## Core Entities

### Station

Represents an airport in the route network.

```kotlin
@Serializable
data class Station(
    val code: AirportCode,
    val name: String,
    val city: String,
    val country: String
)

@Serializable
@JvmInline
value class AirportCode(val value: String) {
    init {
        require(value.length == 3 && value.all { it.isUpperCase() }) {
            "Airport code must be 3 uppercase letters"
        }
    }
}
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| code | AirportCode | 3 uppercase letters (IATA) | Unique airport identifier |
| name | String | Non-empty | Full airport name |
| city | String | Non-empty | City name |
| country | String | Non-empty | Country name |

**Examples**:
- `Station(AirportCode("JED"), "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia")`
- `Station(AirportCode("RUH"), "King Khalid International Airport", "Riyadh", "Saudi Arabia")`

---

### RouteMap

Defines valid origin-destination pairs for flight search.

```kotlin
@Serializable
data class RouteMap(
    val routes: Map<AirportCode, List<AirportCode>>
)
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| routes | Map<AirportCode, List<AirportCode>> | Non-empty | Origin -> Valid destinations |

**Business Rules**:
- A route from A to B does not imply B to A exists
- Routes are unidirectional and must be explicitly defined
- Used to populate destination dropdown based on selected origin

---

### FlightSearchRequest

Request payload for flight search endpoint.

```kotlin
@Serializable
data class FlightSearchRequest(
    val origin: AirportCode,
    val destination: AirportCode,
    val departureDate: LocalDate,
    val passengers: PassengerCounts
)

@Serializable
data class PassengerCounts(
    val adults: Int,
    val children: Int,
    val infants: Int
) {
    init {
        require(adults in 1..9) { "Adults must be between 1 and 9" }
        require(children in 0..8) { "Children must be between 0 and 8" }
        require(infants in 0..adults) { "Infants cannot exceed number of adults" }
        require(adults + children + infants <= 9) { "Total passengers cannot exceed 9" }
    }

    val total: Int get() = adults + children + infants
}
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| origin | AirportCode | Valid station | Departure airport |
| destination | AirportCode | Valid station, != origin | Arrival airport |
| departureDate | LocalDate | Today or future | Travel date (ISO-8601) |
| passengers.adults | Int | 1-9 | Passengers 12+ years |
| passengers.children | Int | 0-8 | Passengers 2-11 years |
| passengers.infants | Int | 0-adults | Passengers 0-1 years |

**Age Category Boundaries** (per clarification):
- Adult: 12+ years
- Child: 2-11 years
- Infant: 0-1 years (under 2)

---

### FlightResponse

Response payload containing search results.

```kotlin
@Serializable
data class FlightResponse(
    val flights: List<Flight>,
    val searchId: String
)

@Serializable
data class Flight(
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val departureTime: Instant,
    val arrivalTime: Instant,
    val durationMinutes: Int,
    val aircraft: String,
    val fareFamilies: List<FareFamily>
)
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| flightNumber | String | Format: XX#### | Airline code + number |
| origin | AirportCode | Valid station | Departure airport |
| destination | AirportCode | Valid station | Arrival airport |
| departureTime | Instant | Future datetime | Scheduled departure (UTC) |
| arrivalTime | Instant | > departureTime | Scheduled arrival (UTC) |
| durationMinutes | Int | > 0 | Flight duration in minutes |
| aircraft | String | Non-empty | Aircraft type (e.g., "A320") |
| fareFamilies | List<FareFamily> | Exactly 3 | Available fare options |
| searchId | String | UUID | Unique search session identifier |

---

### FareFamily

Represents a fare option with pricing and inclusions.

```kotlin
@Serializable
data class FareFamily(
    val code: FareFamilyCode,
    val name: String,
    val price: Money,
    val inclusions: FareInclusions
)

@Serializable
enum class FareFamilyCode {
    FLY,      // Basic
    FLY_PLUS, // Standard with extras
    FLY_MAX   // Premium
}

@Serializable
data class FareInclusions(
    val carryOnBag: BagAllowance,
    val checkedBag: BagAllowance?,
    val seatSelection: SeatSelectionType,
    val changePolicy: ChangePolicy,
    val cancellationPolicy: CancellationPolicy,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
)

@Serializable
data class BagAllowance(
    val pieces: Int,
    val weightKg: Int
)

@Serializable
enum class SeatSelectionType {
    NONE,
    STANDARD,
    PREMIUM
}

@Serializable
data class ChangePolicy(
    val allowed: Boolean,
    val feeAmount: Money?
)

@Serializable
data class CancellationPolicy(
    val allowed: Boolean,
    val refundPercentage: Int
)
```

| Fare Family | Carry-On | Checked Bag | Seat Selection | Changes | Cancellation |
|-------------|----------|-------------|----------------|---------|--------------|
| Fly | 7kg | None | None | Fee | No refund |
| Fly+ | 7kg | 20kg | Standard | Fee | 50% refund |
| FlyMax | 7kg | 30kg | Premium | Free | Full refund |

---

### Money

Value class for monetary amounts.

```kotlin
@Serializable
data class Money(
    val amount: BigDecimal,
    val currency: Currency
)

@Serializable
enum class Currency {
    SAR, // Saudi Riyal (primary)
    USD,
    EUR
}
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| amount | BigDecimal | >= 0 | Monetary value |
| currency | Currency | Valid ISO 4217 | Currency code |

---

### Passenger

Traveler information for booking.

```kotlin
@Serializable
data class Passenger(
    val type: PassengerType,
    val title: Title,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: LocalDate,
    val documentId: String
)

@Serializable
enum class PassengerType {
    ADULT,
    CHILD,
    INFANT
}

@Serializable
enum class Title {
    MR,
    MRS,
    MS,
    MISS,
    MSTR  // Master (for children)
}
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| type | PassengerType | Valid enum | Age category |
| title | Title | Valid enum | Salutation |
| firstName | String | 2-50 chars, alphabetic | Given name |
| lastName | String | 2-50 chars, alphabetic | Family name |
| nationality | String | ISO 3166-1 alpha-2 | Country code |
| dateOfBirth | LocalDate | Valid for passenger type | Birth date |
| documentId | String | 5-20 chars, alphanumeric | Passport/ID number |

**Validation Rules**:
- `firstName` and `lastName` must match passport format (as entered in travel document)
- `dateOfBirth` must align with `PassengerType` per age boundaries
- `documentId` must be valid for the travel date (not expired - deferred to booking)

---

### BookingRequest

Request payload for booking creation.

```kotlin
@Serializable
data class BookingRequest(
    val searchId: String,
    val flightNumber: String,
    val fareFamily: FareFamilyCode,
    val passengers: List<Passenger>,
    val ancillaries: List<Ancillary>,
    val contactEmail: String,
    val payment: PaymentDetails
)

@Serializable
data class Ancillary(
    val type: AncillaryType,
    val passengerIndex: Int,
    val price: Money
)

@Serializable
enum class AncillaryType {
    CHECKED_BAG
}

@Serializable
data class PaymentDetails(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmount: Money
)
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| searchId | String | Valid UUID from search | Links to original search |
| flightNumber | String | From selected flight | Selected flight |
| fareFamily | FareFamilyCode | Valid enum | Selected fare option |
| passengers | List<Passenger> | 1-9, matches search counts | Traveler details |
| ancillaries | List<Ancillary> | Optional | Additional services |
| contactEmail | String | Valid email format | Confirmation delivery |
| payment.cardholderName | String | Non-empty | Name on card |
| payment.cardNumberLast4 | String | 4 digits | Last 4 digits (for receipt) |
| payment.totalAmount | Money | > 0 | Total charged |

---

### BookingConfirmation

Response payload for successful booking.

```kotlin
@Serializable
data class BookingConfirmation(
    val pnr: PnrCode,
    val bookingReference: String,
    val flight: FlightSummary,
    val passengers: List<PassengerSummary>,
    val totalPaid: Money,
    val createdAt: Instant
)

@Serializable
@JvmInline
value class PnrCode(val value: String) {
    init {
        require(value.length == 6 && value.all { it.isUpperCase() || it.isDigit() }) {
            "PNR must be 6 alphanumeric characters"
        }
    }
}

@Serializable
data class FlightSummary(
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val departureTime: Instant,
    val fareFamily: FareFamilyCode
)

@Serializable
data class PassengerSummary(
    val fullName: String,
    val type: PassengerType
)
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| pnr | PnrCode | 6 alphanumeric | Passenger Name Record |
| bookingReference | String | UUID | Internal booking ID |
| flight | FlightSummary | Non-null | Booked flight details |
| passengers | List<PassengerSummary> | Non-empty | Booked passengers |
| totalPaid | Money | > 0 | Amount charged |
| createdAt | Instant | Booking timestamp | When booking was made |

---

### UserSession (Frontend Only)

Local state for the app session.

```kotlin
// Not @Serializable - local state only
data class UserSession(
    val languagePreference: Language,
    val recentSearches: List<FlightSearchRequest>,
    val savedBookings: List<BookingConfirmation>,
    val sessionToken: String?  // For optional account feature
)

enum class Language {
    ENGLISH,
    ARABIC
}
```

| Field | Type | Persistence | Description |
|-------|------|-------------|-------------|
| languagePreference | Language | DataStore | Selected UI language |
| recentSearches | List<FlightSearchRequest> | DataStore | Last 10 searches |
| savedBookings | List<BookingConfirmation> | SQLDelight | Offline-accessible bookings |
| sessionToken | String? | Encrypted DataStore | For optional account login |

---

## Entity Relationships

```
┌─────────────┐
│  RouteMap   │
│  (cached)   │
└──────┬──────┘
       │ validates
       ▼
┌─────────────────────┐
│ FlightSearchRequest │
└──────────┬──────────┘
           │ searches
           ▼
┌─────────────────────┐
│   FlightResponse    │
│   ├── Flight[]      │
│   │   └── FareFamily│
└──────────┬──────────┘
           │ user selects
           ▼
┌─────────────────────┐
│   BookingRequest    │
│   ├── Passenger[]   │
│   ├── Ancillary[]   │
│   └── PaymentDetails│
└──────────┬──────────┘
           │ creates
           ▼
┌─────────────────────┐
│ BookingConfirmation │
│   ├── PnrCode       │
│   ├── FlightSummary │
│   └── PassengerSum[]│
└─────────────────────┘
```

---

## State Transitions

### Booking Flow State

```
┌───────────┐   search   ┌────────────┐   select   ┌──────────────┐
│  SEARCH   │───────────▶│  RESULTS   │───────────▶│   PASSENGER  │
└───────────┘            └────────────┘            └──────┬───────┘
                                                          │ continue
                                                          ▼
┌───────────┐   confirm  ┌────────────┐   pay      ┌──────────────┐
│ CONFIRMED │◀───────────│  PAYMENT   │◀───────────│  ANCILLARIES │
└───────────┘            └────────────┘            └──────────────┘
```

### Payment State

```
┌─────────────┐   submit   ┌────────────┐   success   ┌───────────┐
│   IDLE      │───────────▶│ PROCESSING │────────────▶│  SUCCESS  │
└─────────────┘            └─────┬──────┘             └───────────┘
                                 │ failure
                                 ▼
                           ┌────────────┐
                           │   ERROR    │──▶ retry ──▶ IDLE
                           └────────────┘
```

---

## Validation Summary

| Entity | Field | Rule |
|--------|-------|------|
| AirportCode | value | 3 uppercase letters |
| PassengerCounts | adults | 1-9 |
| PassengerCounts | children | 0-8 |
| PassengerCounts | infants | <= adults |
| PassengerCounts | total | <= 9 |
| Passenger | firstName | 2-50 alphabetic |
| Passenger | lastName | 2-50 alphabetic |
| Passenger | dateOfBirth | Matches PassengerType |
| Passenger | documentId | 5-20 alphanumeric |
| PnrCode | value | 6 alphanumeric |
| Money | amount | >= 0 |
| BookingRequest | contactEmail | Valid email format |
| PaymentDetails | cardNumberLast4 | 4 digits |
