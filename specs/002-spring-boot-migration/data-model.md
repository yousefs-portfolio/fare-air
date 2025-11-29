# Data Model: Spring Boot Migration

**Feature**: 002-spring-boot-migration
**Date**: 2025-11-29

## Overview

This document describes the data model for the Spring Boot backend. **All entities remain unchanged from the Quarkus implementation** as they are defined in the `:shared-contract` module which is NOT modified by this migration.

The data model documentation here serves as a reference for understanding the existing structures that the Spring Boot backend must support.

---

## Module Ownership

| Module | Entities | Changes |
|--------|----------|---------|
| `shared-contract` | All DTOs, Models, API Routes | **None** |
| `backend-spring` | Controller DTOs (request/response wrappers) | Annotation changes only |

---

## Core Entities (shared-contract)

### Station
Represents an airport in the flyadeal network.

| Field | Type | Description |
|-------|------|-------------|
| `code` | `StationCode` (value class) | 3-letter IATA airport code |
| `name` | `String` | Full airport name |
| `city` | `String` | City name |
| `country` | `String` | Country name |

### Flight
Represents a scheduled flight with fare options.

| Field | Type | Description |
|-------|------|-------------|
| `flightNumber` | `String` | Flight number (e.g., F3101) |
| `origin` | `StationCode` | Departure airport |
| `destination` | `StationCode` | Arrival airport |
| `departureTime` | `Instant` | Scheduled departure (UTC) |
| `arrivalTime` | `Instant` | Scheduled arrival (UTC) |
| `durationMinutes` | `Int` | Flight duration |
| `aircraft` | `String` | Aircraft type |
| `fareFamilies` | `List<FareFamily>` | Available fare options |

### FareFamily
Represents a fare option with pricing and inclusions.

| Field | Type | Description |
|-------|------|-------------|
| `code` | `FareFamilyCode` | FLY, FLY_PLUS, FLY_MAX |
| `name` | `String` | Display name |
| `price` | `Money` | Price per passenger |
| `inclusions` | `FareInclusions` | What's included |

### Passenger
Represents a traveler on a booking.

| Field | Type | Description |
|-------|------|-------------|
| `type` | `PassengerType` | ADULT, CHILD, INFANT |
| `title` | `Title` | MR, MRS, MS, MISS, MSTR |
| `firstName` | `String` | First name (2-50 chars) |
| `lastName` | `String` | Last name (2-50 chars) |
| `nationality` | `String` | ISO 3166-1 alpha-2 code |
| `dateOfBirth` | `LocalDate` | Birth date |
| `documentId` | `String` | Passport/ID number |

### Money
Represents a monetary amount.

| Field | Type | Description |
|-------|------|-------------|
| `amountMinor` | `Long` | Amount in minor units (e.g., cents) |
| `currency` | `Currency` | SAR, USD, EUR |

### BookingConfirmation
Represents a completed booking.

| Field | Type | Description |
|-------|------|-------------|
| `pnr` | `PNR` (value class) | 6-character record locator |
| `bookingReference` | `String` | Internal UUID reference |
| `flight` | `FlightSummary` | Booked flight details |
| `passengers` | `List<PassengerSummary>` | Passenger list |
| `totalPaid` | `Money` | Total payment amount |
| `createdAt` | `Instant` | Booking timestamp |

---

## Controller DTOs (backend-spring)

These DTOs are defined in the backend module for request/response mapping. They wrap shared-contract models and add serialization configuration.

### BookingRequestDto
Request body for POST /v1/booking

| Field | Type | Validation |
|-------|------|------------|
| `searchId` | `String` | Required, UUID format |
| `flightNumber` | `String` | Required, pattern ^[A-Z]{2}[0-9]{3,4}$ |
| `fareFamily` | `String` | Required, enum: FLY, FLY_PLUS, FLY_MAX |
| `passengers` | `List<PassengerDto>` | 1-9 items |
| `ancillaries` | `List<AncillaryDto>` | Optional |
| `contactEmail` | `String` | Required, email format |
| `payment` | `PaymentDetailsDto` | Required |

### FlightSearchRequestDto
Request body for POST /v1/search

| Field | Type | Validation |
|-------|------|------------|
| `origin` | `String` | Required, 3 uppercase letters |
| `destination` | `String` | Required, 3 uppercase letters |
| `departureDate` | `String` | Required, ISO-8601 date |
| `passengers` | `PassengerCountsDto` | Required |

### ErrorResponse
Standard error response format.

| Field | Type | Description |
|-------|------|-------------|
| `error` | `String` | Error code (e.g., VALIDATION_ERROR) |
| `message` | `String` | Human-readable message |
| `timestamp` | `String` | ISO-8601 timestamp |
| `details` | `Map<String, Any>?` | Optional field-level errors |

---

## Enumerations

### PassengerType
```
ADULT   - Passengers 12+ years
CHILD   - Passengers 2-11 years
INFANT  - Passengers 0-1 years
```

### FareFamilyCode
```
FLY       - Basic fare
FLY_PLUS  - Enhanced fare
FLY_MAX   - Premium fare
```

### Title
```
MR, MRS, MS, MISS, MSTR
```

### Currency
```
SAR - Saudi Riyal
USD - US Dollar
EUR - Euro
```

### AncillaryType
```
CHECKED_BAG - Additional checked baggage
```

---

## Value Classes (Type Safety)

The shared-contract module uses Kotlin value classes for type safety:

| Class | Wraps | Validation |
|-------|-------|------------|
| `StationCode` | `String` | 3 uppercase letters |
| `PNR` | `String` | 6 alphanumeric characters |

---

## State Transitions

### Booking Lifecycle
```
Search → Selection → Validation → Payment → Confirmation
```

No state is persisted in the current implementation (mock provider). All bookings exist only in memory until application restart.

---

## Validation Rules

### Passenger Validation
- Minimum 1 passenger, maximum 9
- At least 1 adult required
- Infants cannot exceed adults count
- First/last name: 2-50 characters
- Document ID: 5-20 alphanumeric characters

### PNR Validation
- Exactly 6 characters
- Alphanumeric only
- Case-insensitive (normalized to uppercase)

### Search Validation
- Origin ≠ Destination
- Departure date must be in future
- Valid route (origin-destination pair exists)

---

## Migration Notes

**No changes to data model are required for Spring Boot migration.**

The key changes are:
1. **Serialization annotations** - Replace JAX-RS with Jackson annotations where needed
2. **Validation annotations** - Replace Jakarta Bean Validation with Spring Validation (same annotations, different import package)
3. **Date/Time handling** - Configure Jackson to serialize `kotlinx.datetime` types correctly

All model classes in `shared-contract` remain exactly as-is, ensuring frontend compatibility.
