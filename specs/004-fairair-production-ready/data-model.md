# Data Model: FairAir Production Ready

**Feature**: 004-fairair-production-ready
**Date**: 2025-11-29

## Core Entities

### Station (Airport)

Represents an airport where FairAir operates.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| code | String | IATA 3-letter code | Exactly 3 uppercase letters |
| name | String | Airport name | Non-empty |
| city | String | City name | Non-empty |
| country | String | Country name | Non-empty |

**Relationships**: None (reference data)

### Flight

Represents a scheduled flight between two stations.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| flightNumber | String | Flight identifier | Format: "F3 \d{3,4}" |
| departureStation | String | Origin IATA code | Valid station code |
| arrivalStation | String | Destination IATA code | Valid station code |
| departureTime | String | ISO-8601 datetime | Valid datetime |
| arrivalTime | String | ISO-8601 datetime | After departureTime |
| duration | String | Flight duration | Format: "XhYm" |
| fares | List<Fare> | Available fare options | Non-empty |

**Relationships**:
- References Station (departure/arrival)
- Contains Fare list

### Fare

Represents pricing for a specific fare family.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| fareFamily | FareFamily | Tier name | ECONOMY, VALUE, FLEX |
| price | Double | Price in SAR | > 0 |
| currency | String | Currency code | "SAR" |
| baggageAllowance | String | Included baggage | e.g., "7kg carry-on" |
| seatSelection | Boolean | Seat selection included | true/false |
| changeable | Boolean | Flight change allowed | true/false |
| refundable | Boolean | Refund allowed | true/false |

### FareFamily (Enum)

| Value | Display Name | Description |
|-------|--------------|-------------|
| ECONOMY | Economy | Basic fare, carry-on only |
| VALUE | Value | Standard fare, 20kg checked bag |
| FLEX | Flex | Premium fare, full flexibility |

### Passenger

Traveler details for booking.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| firstName | String | Given name | Non-empty, letters only |
| lastName | String | Family name | Non-empty, letters only |
| email | String | Contact email | Valid email format |
| phone | String | Phone number | Valid phone format |
| passengerType | PassengerType | Passenger category | ADULT, CHILD, INFANT |

### PassengerType (Enum)

| Value | Age Range |
|-------|-----------|
| ADULT | 12+ years |
| CHILD | 2-11 years |
| INFANT | 0-1 years |

### Booking

A confirmed flight reservation.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| confirmationCode | String | Unique reference | 6 alphanumeric chars |
| flight | Flight | Selected flight | Valid flight |
| fareFamily | FareFamily | Selected fare tier | Valid enum value |
| passengers | List<Passenger> | Travelers | 1-9 passengers |
| totalPrice | Double | Total cost | Sum of fares |
| currency | String | Currency | "SAR" |
| bookedAt | String | Booking timestamp | ISO-8601 datetime |
| status | BookingStatus | Current status | Valid enum value |

### BookingStatus (Enum)

| Value | Description |
|-------|-------------|
| CONFIRMED | Booking is confirmed |
| CANCELLED | Booking was cancelled |
| COMPLETED | Flight has departed |

## State Transitions

### Booking Flow State Machine

```
[Initial] → (search) → [SearchResults]
    ↓
(select flight) → [FareSelection]
    ↓
(select fare) → [PassengerDetails]
    ↓
(enter details) → [Review]
    ↓
(confirm) → [Confirmation]
```

### UI State Pattern

All screens follow consistent state pattern:

```kotlin
sealed class ScreenState<T> {
    object Loading : ScreenState<Nothing>
    data class Content<T>(val data: T) : ScreenState<T>
    data class Error(val message: String) : ScreenState<Nothing>
}
```

## Color Scheme (Brand)

### FairAir Colors

| Name | Hex | Usage |
|------|-----|-------|
| Primary | #0D9488 | Teal - buttons, headers, primary actions |
| Accent | #F97316 | Coral - highlights, CTAs, important info |
| BackgroundDeep | #1E293B | Dark slate - main background |
| BackgroundMid | #334155 | Medium slate - cards, elevated surfaces |
| GlassBg | #1E293B80 | Semi-transparent glass effect |
| TextMain | #FFFFFF | Primary text |
| TextMuted | #94A3B8 | Secondary text |
| GradientStart | #0F766E | Darker teal - gradient start |
| GradientEnd | #1E293B | Slate - gradient end |
| Success | #22C55E | Green - success states |
| Error | #EF4444 | Red - error states |
| Warning | #F59E0B | Amber - warning states |

## Validation Rules

### Search Input
- Origin ≠ Destination
- Date ≥ Today
- Passengers: 1-9 total, infants ≤ adults

### Passenger Details
- Names: Letters, spaces, hyphens only (2-50 chars)
- Email: Standard email format
- Phone: Digits, +, spaces (7-15 chars)

### Booking
- At least 1 adult passenger
- Infants require adult (1:1 ratio max)
