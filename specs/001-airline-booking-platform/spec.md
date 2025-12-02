# Feature Specification: Airline Booking Platform (Project Shadow)

**Feature Branch**: `001-airline-booking-platform`
**Created**: 2025-11-25
**Status**: Draft
**Input**: User description: "Full-stack Airline Booking Platform using Kotlin Multiplatform with Quarkus backend and Compose Multiplatform frontend for fairair in-housing"

## Clarifications

### Session 2025-11-25

- Q: What is the user authentication strategy? → A: Guest checkout with optional account - users can book without login and optionally save PNR to account later.
- Q: What are the passenger age category boundaries? → A: Industry standard - Adult 12+, Child 2-11, Infant 0-1 (under 2 years).
- Q: What is the payment integration scope? → A: Mock payment with real validation - validate card format (Luhn, expiry, CVV), mock transaction processing.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search Available Flights (Priority: P1)

A traveler opens the fairair app to search for flights between two cities. They select their departure city, and the app intelligently filters destination options to show only valid route connections. They pick a destination, select their travel date, specify the number of passengers, and initiate a search. The system displays available flights with departure times, arrival times, flight duration, and pricing.

**Why this priority**: Flight search is the core entry point for all bookings. Without the ability to search flights, no other functionality in the booking flow can be accessed. This represents the minimum viable product.

**Independent Test**: Can be fully tested by launching the app, selecting origin/destination/date, searching, and verifying flight results display correctly. Delivers immediate value by allowing users to browse flight options.

**Acceptance Scenarios**:

1. **Given** a user is on the search screen and the app has loaded available routes, **When** the user selects "JED" as origin, **Then** the destination dropdown displays only cities with valid flight connections from JED (e.g., RUH, DMM) and excludes invalid destinations.

2. **Given** a user has selected valid origin, destination, date, and passenger count, **When** the user taps "Search Flights", **Then** the system displays a loading indicator (shimmer/skeleton) followed by a list of available flights with departure time, arrival time, duration, and base price.

3. **Given** a user initiates a flight search, **When** the backend takes more than expected time to respond, **Then** the user sees a skeleton loading animation until results arrive, maintaining engagement.

4. **Given** a user searches for flights on a route with no availability, **When** results return empty, **Then** the system displays a friendly "No flights available" message with suggestions to try alternative dates.

---

### User Story 2 - Select Fare Family and View Flight Details (Priority: P1)

After viewing search results, a traveler wants to compare fare options before booking. They tap on a specific flight to expand details and see three fare family options: "Fly" (basic), "Fly+" (standard with extras), and "FlyMax" (premium with maximum flexibility). Each fare family clearly displays included perks, restrictions, and the total price.

**Why this priority**: Fare selection is essential to complete any booking. Airlines generate significant revenue through fare family upsells, making this a critical business function tied directly to the search results.

**Independent Test**: Can be tested by searching for flights, tapping a result, and verifying fare family options display with correct pricing and perks. Delivers value by enabling informed purchase decisions.

**Acceptance Scenarios**:

1. **Given** a user is viewing flight search results, **When** they tap on a flight card, **Then** the card expands to reveal three fare family options (Fly, Fly+, FlyMax) with their respective prices and included features.

2. **Given** a user is viewing expanded fare options, **When** they review each fare family, **Then** they see a clear comparison of included baggage allowance, seat selection options, change/cancellation policies, and priority services.

3. **Given** a user has selected a fare family, **When** they confirm their selection, **Then** the system navigates to the passenger details screen with the selected flight and fare information preserved.

---

### User Story 3 - Enter Passenger Information (Priority: P2)

A traveler proceeds to enter passenger details for their booking. They fill in required information for each passenger including title, first name, last name, nationality, date of birth, and travel document ID. The form validates entries in real-time and prevents progression until all required fields are correctly completed.

**Why this priority**: Passenger information is required for booking completion but depends on successful flight search and fare selection. It's essential for regulatory compliance and boarding pass generation.

**Independent Test**: Can be tested by navigating to the passenger form, entering valid and invalid data combinations, and verifying validation behavior. Delivers value by capturing required traveler data for booking completion.

**Acceptance Scenarios**:

1. **Given** a user is on the passenger details screen, **When** they view the form, **Then** they see fields for Title (dropdown), First Name, Last Name, Nationality (dropdown), Date of Birth (date picker), and Document ID with clear labels.

2. **Given** a user is entering passenger details, **When** they leave a mandatory field empty or enter invalid data, **Then** the system displays an inline validation error explaining what is required.

3. **Given** all passenger details are correctly filled, **When** the user taps "Continue", **Then** they proceed to the ancillaries/add-ons screen.

4. **Given** the booking includes multiple passengers (e.g., 2 adults, 1 child), **When** the user views the passenger form, **Then** they see separate form sections for each passenger with appropriate labels (Passenger 1, Passenger 2, etc.).

---

### User Story 4 - Add Ancillary Services (Priority: P2)

A traveler wants to add extra services to their booking. They see an option to add checked baggage for an additional fee. They toggle the option on, see the updated total price, and proceed to payment.

**Why this priority**: Ancillaries represent significant ancillary revenue for airlines. While optional, they enhance the booking experience and increase average booking value.

**Independent Test**: Can be tested by reaching the ancillaries screen, toggling baggage option, and verifying price updates correctly. Delivers value by enabling revenue-generating add-on purchases.

**Acceptance Scenarios**:

1. **Given** a user is on the ancillaries screen, **When** they view available add-ons, **Then** they see a toggle option for "Add Checked Bag" with the price clearly displayed (e.g., "+100 SAR").

2. **Given** a user toggles the checked bag option ON, **When** the toggle state changes, **Then** the displayed total price updates immediately to reflect the addition.

3. **Given** a user toggles the checked bag option OFF after previously enabling it, **When** the toggle state changes, **Then** the total price decreases by the baggage fee amount.

---

### User Story 5 - Complete Payment (Priority: P2)

A traveler is ready to pay for their booking. They enter credit card details into a secure payment form and confirm the transaction. Upon successful payment, they receive a booking confirmation with their PNR (Passenger Name Record).

**Why this priority**: Payment completion is the final step in the booking funnel. Without payment processing, no revenue is generated. It directly enables the business model.

**Independent Test**: Can be tested by navigating through the booking flow to payment, entering mock card details, and verifying successful booking creation. Delivers value by converting browsing users into paying customers.

**Acceptance Scenarios**:

1. **Given** a user is on the payment screen, **When** they view the form, **Then** they see fields for card number, expiry date, CVV, and cardholder name with appropriate input formatting.

2. **Given** a user has entered valid payment details, **When** they tap "Pay Now", **Then** the system displays a processing indicator, submits the booking request, and upon success navigates to the confirmation screen.

3. **Given** payment processing fails, **When** the error response is received, **Then** the user sees a clear error message explaining the issue (e.g., "Card declined") with an option to retry.

---

### User Story 6 - View Booking Confirmation (Priority: P2)

A traveler has successfully completed a booking. They see a confirmation screen displaying their unique PNR code, flight details, and passenger summary. They have an option to save the booking locally for offline access.

**Why this priority**: Confirmation provides closure to the booking experience and delivers the essential PNR that travelers need for check-in and airport procedures. Essential for user satisfaction and operational continuity.

**Independent Test**: Can be tested by completing a successful booking and verifying confirmation details display correctly. Delivers value by providing travelers their booking reference.

**Acceptance Scenarios**:

1. **Given** a booking has been successfully completed, **When** the user views the confirmation screen, **Then** they see the PNR code prominently displayed, along with flight details (route, date, time) and passenger names.

2. **Given** a user is on the confirmation screen, **When** they tap "Save to Home", **Then** the booking details are stored locally and accessible from the home screen even when offline.

---

### User Story 7 - Switch App Language (Priority: P3)

A traveler prefers to use the app in Arabic. They access language settings and switch from English to Arabic. The entire app interface immediately updates to display Arabic text with right-to-left (RTL) layout, including proper text alignment, navigation flow, and UI element positioning.

**Why this priority**: fairair serves a predominantly Arabic-speaking market in Saudi Arabia. Language support is essential for market accessibility and user experience but not blocking for core booking functionality.

**Independent Test**: Can be tested by changing language settings and verifying all UI elements, text, and layout direction update correctly. Delivers value by making the app accessible to Arabic-speaking users.

**Acceptance Scenarios**:

1. **Given** a user is using the app in English, **When** they change the language setting to Arabic, **Then** all UI text changes to Arabic and the entire layout flips to right-to-left orientation.

2. **Given** the app is in Arabic RTL mode, **When** the user navigates through screens, **Then** navigation animations, scrolling direction, and UI element placement all respect RTL conventions.

3. **Given** a user switches language mid-session, **When** the change is applied, **Then** their current navigation state and any entered data are preserved.

---

### User Story 8 - App Initialization and Offline Access (Priority: P3)

A traveler launches the app for the first time or after being offline. The app fetches and caches the route network configuration, enabling the search screen to function correctly. Previously saved booking confirmations remain accessible even without network connectivity.

**Why this priority**: App initialization is foundational infrastructure. Offline access enhances reliability for travelers in areas with poor connectivity but is not essential for the primary booking flow.

**Independent Test**: Can be tested by launching the app fresh, verifying routes load, then enabling airplane mode and accessing saved bookings. Delivers value by ensuring app reliability and offline utility.

**Acceptance Scenarios**:

1. **Given** a user launches the app for the first time, **When** the app initializes, **Then** it fetches and caches the valid route network from the backend before displaying the search screen.

2. **Given** a user has previously saved a booking confirmation, **When** they open the app without network connectivity, **Then** they can still view their saved boarding pass and booking details.

3. **Given** route data is cached, **When** the user opens the app subsequently, **Then** the search screen loads immediately using cached data while optionally refreshing in the background.

---

### Edge Cases

- What happens when a user's session expires during the booking flow? The system preserves entered data locally and prompts re-authentication without losing progress.
- How does the system handle duplicate booking attempts (user tapping "Pay" multiple times)? The payment button disables immediately on first tap and shows processing state to prevent duplicate submissions.
- What happens if the backend returns malformed or unexpected data? The app displays a generic error screen with retry option rather than crashing, and logs the error for debugging.
- How does the app behave on extremely slow network connections? Timeout handling displays appropriate messages, and critical cached data remains functional.
- What happens when a flight becomes unavailable between search and booking? The booking request returns an error, and the user is directed back to search with an explanation that the flight is no longer available.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display valid destination airports based on the selected origin airport using the route network configuration.
- **FR-002**: System MUST fetch and cache the route network configuration on app launch with automatic background refresh.
- **FR-003**: System MUST allow users to search flights by specifying origin, destination, travel date, and passenger count.
- **FR-004**: System MUST display flight search results showing departure time, arrival time, flight duration, and fare prices.
- **FR-005**: System MUST display a loading skeleton (shimmer effect) while flight search results are being fetched.
- **FR-006**: System MUST allow users to expand a flight card to view and select from three fare families: Fly, Fly+, and FlyMax.
- **FR-007**: System MUST display fare family perks including baggage allowance, seat selection, and change/cancellation policies.
- **FR-008**: System MUST collect passenger information including title, first name, last name, nationality, date of birth, and document ID for each traveler.
- **FR-009**: System MUST validate passenger form fields in real-time and prevent submission with invalid or missing data.
- **FR-010**: System MUST allow users to add checked baggage as an ancillary service with dynamic price updates.
- **FR-011**: System MUST provide a payment form accepting credit card details (card number, expiry, CVV, cardholder name) with client-side validation (Luhn algorithm for card number, expiry date format, CVV length) and mock transaction processing.
- **FR-012**: System MUST submit booking requests to the backend and display the returned PNR on successful completion.
- **FR-013**: System MUST display clear error messages when booking or payment fails with retry options.
- **FR-014**: System MUST allow users to save booking confirmations locally for offline access.
- **FR-015**: System MUST support English and Arabic languages with full RTL layout support for Arabic.
- **FR-016**: System MUST allow runtime language switching without losing user session or navigation state.
- **FR-017**: System MUST persist recent search history and cached boarding passes locally without requiring authentication.
- **FR-021**: System MUST allow users to complete the entire booking flow as a guest (no account required), requiring only email for booking confirmation delivery.
- **FR-022**: System MUST provide an optional account creation flow after booking completion, allowing users to save their PNR to an account for future access.
- **FR-018**: System MUST handle Android back button navigation correctly, integrating with the app's navigation stack.
- **FR-019**: System MUST respect iOS safe area insets for proper display on devices with notches and home indicators.
- **FR-020**: System MUST function across Android, iOS, and Web (Wasm) platforms from a single codebase.

### Key Entities

- **Station**: Represents an airport with a code (e.g., "JED", "RUH") and full name (e.g., "Jeddah - King Abdulaziz International Airport"). Used in route mapping and search selection.

- **RouteMap**: Defines valid origin-destination pairs. Maps each origin station to a list of allowed destination stations, ensuring only valid routes are searchable.

- **FlightSearchRequest**: Captures search parameters including origin station, destination station, departure date (ISO-8601), and passenger counts by type: Adult (12+ years), Child (2-11 years), Infant (0-1 years, under 2).

- **FlightResponse**: Contains flight search results including flight number, departure/arrival times, duration, aircraft type, and available fare families with pricing.

- **FareFamily**: Represents a fare option (Fly, Fly+, FlyMax) with associated price, included baggage, seat selection options, and change/cancellation policies.

- **Passenger**: Contains traveler information including title, first name, last name, nationality, date of birth, and travel document ID. Associated with a booking.

- **BookingRequest**: Combines selected flight, fare family, passenger list, and ancillary selections for submission to the booking system.

- **BookingConfirmation**: Contains the successful booking result including PNR code, flight details, passenger summary, and payment receipt reference.

- **UserSession**: Maintains authentication state, user preferences (language, recent searches), and locally cached booking confirmations.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete a flight search and view results in under 5 seconds on standard network conditions.
- **SC-002**: Users can complete the entire booking flow (search to confirmation) in under 5 minutes.
- **SC-003**: 95% of search results display within 3 seconds of user initiating the search.
- **SC-004**: The app supports simultaneous usage across 10,000 concurrent users without service degradation.
- **SC-005**: 90% of users successfully complete their first booking attempt without encountering blocking errors.
- **SC-006**: Language switching completes in under 1 second with full RTL layout transformation.
- **SC-007**: Saved booking confirmations remain accessible offline with 100% data integrity.
- **SC-008**: The app launches and displays the search screen in under 3 seconds on supported devices.
- **SC-009**: Zero critical crashes during standard booking flows on production releases.
- **SC-010**: Payment form validation prevents 100% of improperly formatted card submissions.
