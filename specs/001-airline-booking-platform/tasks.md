# Tasks: Airline Booking Platform (Project Shadow)

**Input**: Design documents from `/specs/001-airline-booking-platform/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml

**Tests**: Tests are NOT explicitly requested in the feature specification. Implementation-focused tasks only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **:shared-contract**: `shared-contract/src/commonMain/kotlin/com/fairair/shared/`
- **:backend-quarkus**: `backend-quarkus/src/main/kotlin/com/fairair/backend/`
- **:apps-kmp**: `apps-kmp/src/commonMain/kotlin/com/fairair/app/`
- **Platform-specific**: `apps-kmp/src/{androidMain,iosMain,wasmJsMain}/kotlin/com/fairair/app/`

---

## Phase 1: Setup (Project Scaffolding)

**Purpose**: Initialize Gradle multi-module monorepo with Kotlin 2.0 and all dependencies

- [ ] T001 Create root `build.gradle.kts` with Kotlin 2.0 K2 compiler configuration
- [ ] T002 Create `settings.gradle.kts` defining :shared-contract, :backend-quarkus, :apps-kmp modules
- [ ] T003 Create `gradle/libs.versions.toml` with version catalog for all dependencies (Quarkus, Compose MP, Voyager, Koin, Ktor, kotlinx.serialization, kotlinx.datetime, Caffeine)
- [ ] T004 [P] Create :shared-contract module `shared-contract/build.gradle.kts` with KMP configuration (commonMain only, no platform-specific deps)
- [ ] T005 [P] Create :backend-quarkus module `backend-quarkus/build.gradle.kts` with Quarkus and kotlinx.serialization plugins
- [ ] T006 [P] Create :apps-kmp module `apps-kmp/build.gradle.kts` with Compose Multiplatform, Voyager, Koin, Ktor Client for Android/iOS/Wasm targets
- [ ] T007 Create `backend-quarkus/src/main/resources/application.properties` with server config, CORS, and fairair.provider=mock setting
- [ ] T008 Verify project builds with `./gradlew build` - all modules compile successfully

**Checkpoint**: Project structure ready - all modules compile

---

## Phase 2: Foundational (Shared Contract & Backend Core)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

### Shared Contract Models (DTOs)

- [ ] T009 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/AirportCode.kt` with value class and validation
- [ ] T010 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/Station.kt` with @Serializable data class
- [ ] T011 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/RouteMap.kt` with @Serializable data class
- [ ] T012 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/Money.kt` with @Serializable data class and Currency enum
- [ ] T013 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/PassengerCounts.kt` with validation (adults 1-9, children 0-8, infants <= adults)
- [ ] T014 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/FareFamily.kt` with FareFamilyCode enum, FareInclusions, BagAllowance, SeatSelectionType, ChangePolicy, CancellationPolicy
- [ ] T015 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/Passenger.kt` with PassengerType enum, Title enum, and validation rules
- [ ] T016 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/Ancillary.kt` with AncillaryType enum
- [ ] T017 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/models/PnrCode.kt` with value class and 6-char alphanumeric validation

### Shared Contract DTOs (Request/Response)

- [ ] T018 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/dtos/FlightSearchRequest.kt` with origin, destination, departureDate, passengers
- [ ] T019 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/dtos/FlightResponse.kt` with Flight data class containing fareFamilies and searchId
- [ ] T020 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/dtos/BookingRequest.kt` with searchId, flightNumber, fareFamily, passengers, ancillaries, contactEmail, payment
- [ ] T021 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/dtos/BookingConfirmation.kt` with pnr, bookingReference, flight, passengers, totalPaid, createdAt
- [ ] T022 [P] Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/dtos/ErrorResponse.kt` with error, message, timestamp, details

### Shared Contract API Routes

- [ ] T023 Create `shared-contract/src/commonMain/kotlin/com/fairair/shared/api/ApiRoutes.kt` with constants for /v1/config/routes, /v1/config/stations, /v1/search, /v1/booking, /v1/booking/{pnr}

### Backend Infrastructure

- [ ] T024 Create `backend-quarkus/src/main/kotlin/com/fairair/backend/exception/GlobalExceptionHandler.kt` with ExceptionMapper for clean 400/500 JSON responses
- [ ] T025 Create `backend-quarkus/src/main/kotlin/com/fairair/backend/cache/CacheConfig.kt` with Caffeine caches (routes 24h TTL, search 5min TTL)
- [ ] T026 Create `backend-quarkus/src/main/kotlin/com/fairair/backend/client/NavitaireClient.kt` interface with getRoutes(), searchFlights(), createBooking() methods
- [ ] T027 [P] Create `backend-quarkus/src/main/resources/mock-data/navitaire_routes.json` with valid O-D pairs (JED->RUH, RUH->JED, JED->DMM, etc.)
- [ ] T028 [P] Create `backend-quarkus/src/main/resources/mock-data/navitaire_availability.json` with sample flight results including fare families
- [ ] T029 [P] Create `backend-quarkus/src/main/resources/mock-data/navitaire_pnr.json` with sample booking confirmation data
- [ ] T030 Create `backend-quarkus/src/main/kotlin/com/fairair/backend/client/MockNavitaireClient.kt` implementing NavitaireClient, reading JSON files with 0.5-1.5s configurable delay
- [ ] T031 Create `backend-quarkus/src/main/kotlin/com/fairair/backend/client/RealNavitaireClient.kt` skeleton implementing NavitaireClient (placeholder for future external API)

### Frontend Infrastructure

- [ ] T032 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/theme/Color.kt` with fairair brand colors (Purple primary, Lime Green accent)
- [ ] T033 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/theme/Typography.kt` with font families supporting Latin and Arabic scripts
- [ ] T034 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/theme/FairairTheme.kt` with Material 3 theme wrapping colors and typography
- [ ] T035 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/di/AppModule.kt` with Koin module definitions for repositories, network client, and screen models
- [ ] T036 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/network/ApiClient.kt` with Ktor HttpClient configuration (ContentNegotiation, timeouts, base URL)
- [ ] T037 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/navigation/AppNavigator.kt` with Voyager Navigator setup and screen registry
- [ ] T038 Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/App.kt` main composable with Koin initialization, theme wrapper, and Navigator

### Platform Entry Points

- [ ] T039 [P] Create `apps-kmp/src/androidMain/kotlin/com/fairair/app/MainActivity.kt` with setContent calling App()
- [ ] T040 [P] Create `apps-kmp/src/androidMain/kotlin/com/fairair/app/MainApplication.kt` with Koin startKoin() initialization
- [ ] T041 [P] Create `apps-kmp/src/androidMain/AndroidManifest.xml` with INTERNET permission and activity declaration
- [ ] T042 [P] Create `apps-kmp/src/iosMain/kotlin/com/fairair/app/MainViewController.kt` with ComposeUIViewController entry point
- [ ] T043 [P] Create `apps-kmp/src/wasmJsMain/kotlin/com/fairair/app/Main.kt` with CanvasBasedWindow entry point and Js engine Ktor client

**Checkpoint**: Foundation ready - backend can serve routes, frontend can display themed UI

---

## Phase 3: User Story 1 - Search Available Flights (Priority: P1)

**Goal**: Users can search for flights between valid origin-destination pairs and view results

**Independent Test**: Launch app, select JED as origin, verify only valid destinations appear, select RUH, pick date, search, see flight results with shimmer loading

### Backend for US1

- [ ] T044 [US1] Create `backend-quarkus/src/main/kotlin/com/fairair/backend/service/FlightService.kt` with getRoutes() (cached 24h) and searchFlights() (cached 5min) methods transforming Navitaire responses to clean DTOs
- [ ] T045 [US1] Create `backend-quarkus/src/main/kotlin/com/fairair/backend/api/ConfigResource.kt` with GET /v1/config/routes and GET /v1/config/stations endpoints
- [ ] T046 [US1] Create `backend-quarkus/src/main/kotlin/com/fairair/backend/api/SearchResource.kt` with POST /v1/search endpoint accepting FlightSearchRequest, returning FlightResponse

### Frontend Components for US1

- [ ] T047 [P] [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/SkeletonLoader.kt` with shimmer animation composable
- [ ] T048 [P] [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/AirportSelector.kt` with dropdown filtering destinations based on selected origin
- [ ] T049 [P] [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/DatePicker.kt` with date selection (today or future only)
- [ ] T050 [P] [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/PassengerCounter.kt` with adult/child/infant counters and validation

### Frontend Repository for US1

- [ ] T051 [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/repository/StationRepository.kt` with fetchRoutes(), getStations(), getDestinationsForOrigin() using ApiClient
- [ ] T052 [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/repository/FlightRepository.kt` with searchFlights() using ApiClient

### Frontend Screen for US1

- [ ] T053 [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/search/SearchScreenModel.kt` with UiState (Loading/Content/Error), route loading on init, destination filtering, search action
- [ ] T054 [US1] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/search/SearchScreen.kt` with origin selector, destination selector (filtered), date picker, passenger counter, search button (disabled until valid)

**Checkpoint**: US1 complete - users can search flights and see results with loading states

---

## Phase 4: User Story 2 - Select Fare Family (Priority: P1)

**Goal**: Users can view flight details, compare fare families, and select one to proceed

**Independent Test**: After searching, tap a flight card, see Fly/Fly+/FlyMax options with prices and perks, select one, navigate to passenger form

### Frontend Components for US2

- [ ] T055 [P] [US2] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/FlightCard.kt` with departure/arrival times, duration, base price, expandable state
- [ ] T056 [P] [US2] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/FareFamilySelector.kt` with three fare options showing inclusions (baggage, seat, change/cancel policies)

### Frontend Screen for US2

- [ ] T057 [US2] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/results/ResultsScreenModel.kt` with flight list state, selected flight expansion, fare family selection, navigation to passenger screen
- [ ] T058 [US2] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/results/ResultsScreen.kt` with LazyColumn of FlightCards, expanded FareFamilySelector, continue button

**Checkpoint**: US1+US2 complete - users can search and select fare families

---

## Phase 5: User Story 3 - Enter Passenger Information (Priority: P2)

**Goal**: Users can enter passenger details with real-time validation

**Independent Test**: On passenger screen, see form for each passenger, enter invalid data to see errors, enter valid data to enable continue button

### Frontend Components for US3

- [ ] T059 [P] [US3] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/PassengerForm.kt` with Title dropdown, FirstName, LastName, Nationality dropdown, DOB picker, DocumentId fields
- [ ] T060 [P] [US3] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/ValidationUtils.kt` with name validation (2-50 chars, alphabetic), documentId validation (5-20 alphanumeric), DOB age validation

### Frontend Screen for US3

- [ ] T061 [US3] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/passenger/PassengerScreenModel.kt` with passenger list state, validation logic per passenger, navigation to ancillaries
- [ ] T062 [US3] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/passenger/PassengerScreen.kt` with scrollable list of PassengerForms, continue button (disabled until all valid)

**Checkpoint**: US1+US2+US3 complete - users can search, select fare, enter passengers

---

## Phase 6: User Story 4 - Add Ancillary Services (Priority: P2)

**Goal**: Users can add checked baggage and see dynamic price updates

**Independent Test**: On ancillaries screen, toggle baggage, see total price update, proceed to payment

### Frontend Screen for US4

- [ ] T063 [US4] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/ancillaries/AncillariesScreenModel.kt` with baggage toggle state per passenger, price calculation, navigation to payment
- [ ] T064 [US4] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/ancillaries/AncillariesScreen.kt` with baggage toggle per passenger (+100 SAR), running total display, continue button

**Checkpoint**: US1-US4 complete - full flow to payment screen

---

## Phase 7: User Story 5 - Complete Payment (Priority: P2)

**Goal**: Users can enter card details with validation and submit booking

**Independent Test**: On payment screen, enter invalid card to see errors, enter valid mock card, tap Pay Now, see processing state, receive PNR

### Backend for US5

- [ ] T065 [US5] Create `backend-quarkus/src/main/kotlin/com/fairair/backend/service/BookingService.kt` with createBooking() method generating PNR, validating request, returning BookingConfirmation
- [ ] T066 [US5] Create `backend-quarkus/src/main/kotlin/com/fairair/backend/api/BookingResource.kt` with POST /v1/booking and GET /v1/booking/{pnr} endpoints

### Frontend Components for US5

- [ ] T067 [P] [US5] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/CreditCardForm.kt` with card number (Luhn validation), expiry (MM/YY format), CVV (3-4 digits), cardholder name
- [ ] T068 [P] [US5] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/components/PaymentValidation.kt` with Luhn algorithm, expiry date validation, CVV length check

### Frontend Repository for US5

- [ ] T069 [US5] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/repository/BookingRepository.kt` with createBooking() and getBooking() using ApiClient

### Frontend Screen for US5

- [ ] T070 [US5] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/payment/PaymentScreenModel.kt` with card validation state, processing state, booking submission, navigation to confirmation
- [ ] T071 [US5] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/payment/PaymentScreen.kt` with CreditCardForm, total summary, Pay Now button (disables on tap), processing indicator

**Checkpoint**: US1-US5 complete - full booking flow works

---

## Phase 8: User Story 6 - View Booking Confirmation (Priority: P2)

**Goal**: Users see PNR and booking details, can save locally for offline access

**Independent Test**: After payment success, see confirmation screen with PNR, tap Save to Home, access saved booking offline

### Frontend Persistence for US6

- [ ] T072 [US6] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/persistence/LocalStorage.kt` with expect/actual for DataStore/SQLDelight to save BookingConfirmation

### Frontend Screen for US6

- [ ] T073 [US6] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/confirmation/ConfirmationScreenModel.kt` with booking details state, save to local storage action
- [ ] T074 [US6] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/confirmation/ConfirmationScreen.kt` with prominent PNR display, flight summary, passenger list, Save to Home button

**Checkpoint**: US1-US6 complete - full booking flow with confirmation and save

---

## Phase 9: User Story 7 - Switch App Language (Priority: P3)

**Goal**: Users can switch between English and Arabic with full RTL support

**Independent Test**: Open settings, switch to Arabic, verify all text changes and layout flips RTL

### Frontend Localization for US7

- [ ] T075 [P] [US7] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/localization/Strings.kt` with string resources for English and Arabic (all UI labels)
- [ ] T076 [P] [US7] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/localization/LocalizationManager.kt` with CompositionLocal for language, RTL detection, language switching

### Frontend Screen for US7

- [ ] T077 [US7] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/settings/SettingsScreen.kt` with language toggle (English/Arabic)
- [ ] T078 [US7] Update `apps-kmp/src/commonMain/kotlin/com/fairair/app/App.kt` to wrap content with CompositionLocalProvider for LocalLayoutDirection based on language

**Checkpoint**: US1-US7 complete - booking flow works in English and Arabic

---

## Phase 10: User Story 8 - App Initialization and Offline Access (Priority: P3)

**Goal**: App fetches routes on launch, saved bookings accessible offline

**Independent Test**: Fresh app launch shows routes loaded, save a booking, enable airplane mode, access saved booking

### Frontend Persistence for US8

- [ ] T079 [US8] Update `apps-kmp/src/commonMain/kotlin/com/fairair/app/persistence/LocalStorage.kt` with route caching, search history (last 10), session preferences

### Frontend Screen for US8

- [ ] T080 [US8] Create `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/saved/SavedBookingsScreen.kt` with list of locally saved BookingConfirmations
- [ ] T081 [US8] Update `apps-kmp/src/commonMain/kotlin/com/fairair/app/screens/search/SearchScreenModel.kt` to load routes from cache on init, refresh in background

**Checkpoint**: US1-US8 complete - all user stories implemented

---

## Phase 11: Platform Specifics (Android, iOS, Web)

**Purpose**: Platform-specific polish

### Android

- [ ] T082 [P] Update `apps-kmp/src/androidMain/kotlin/com/fairair/app/MainActivity.kt` with Voyager back handler integration for Android system back button

### iOS

- [ ] T083 [P] Update `apps-kmp/src/iosMain/kotlin/com/fairair/app/MainViewController.kt` with WindowInsets handling for safe area

### Web (Wasm)

- [ ] T084 [P] Update `backend-quarkus/src/main/resources/application.properties` with CORS configuration for localhost:8081 (Wasm dev server)
- [ ] T085 [P] Verify `apps-kmp/src/wasmJsMain/kotlin/com/fairair/app/Main.kt` uses Js engine for Ktor client

**Checkpoint**: All platforms work correctly

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements affecting multiple user stories

- [ ] T086 Add comprehensive error handling to all ScreenModels with user-friendly messages
- [ ] T087 Implement retry logic in ApiClient for transient network failures
- [ ] T088 Add logging throughout backend services for observability
- [ ] T089 Run quickstart.md validation - verify all build commands work
- [ ] T090 Verify all acceptance scenarios from spec.md pass manual testing

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 - BLOCKS all user stories
- **Phase 3-10 (User Stories)**: All depend on Phase 2 completion
- **Phase 11 (Platform)**: Can run after Phase 2, parallel with user stories
- **Phase 12 (Polish)**: Depends on all user story phases

### User Story Dependencies

- **US1 (Search)**: Can start after Phase 2 - No dependencies on other stories
- **US2 (Fare Selection)**: Depends on US1 (needs search results to display)
- **US3 (Passenger)**: Depends on US2 (needs selected flight/fare)
- **US4 (Ancillaries)**: Depends on US3 (needs passenger data)
- **US5 (Payment)**: Depends on US4 (needs full booking data)
- **US6 (Confirmation)**: Depends on US5 (needs completed booking)
- **US7 (Language)**: Can start after Phase 2 - independent of booking flow
- **US8 (Offline)**: Can start after Phase 2 - independent of booking flow

### Within Each User Story

- Backend tasks before frontend screens that depend on them
- Models/Components before Screens that use them
- ScreenModels before Screens

### Parallel Opportunities

**Phase 1 (Setup)**:
```bash
# Run T004, T005, T006 in parallel (different module build files)
```

**Phase 2 (Foundational)**:
```bash
# Run all T009-T017 in parallel (different model files)
# Run all T018-T022 in parallel (different DTO files)
# Run T027, T028, T029 in parallel (different mock data files)
# Run T032, T033 in parallel (different theme files)
# Run T039-T043 in parallel (different platform entry points)
```

**User Story Phases**:
```bash
# Within each story, tasks marked [P] can run in parallel
# US7 and US8 can run in parallel with US3-US6 if staffed
```

---

## Parallel Example: User Story 1

```bash
# Phase 3: Launch component tasks in parallel
Task: "[P] [US1] Create SkeletonLoader.kt"
Task: "[P] [US1] Create AirportSelector.kt"
Task: "[P] [US1] Create DatePicker.kt"
Task: "[P] [US1] Create PassengerCounter.kt"

# Then sequentially:
Task: "[US1] Create StationRepository.kt"
Task: "[US1] Create FlightRepository.kt"
Task: "[US1] Create SearchScreenModel.kt"
Task: "[US1] Create SearchScreen.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Search)
4. **STOP and VALIDATE**: Test flight search independently
5. Deploy/demo search functionality as MVP

### Incremental Delivery

1. **Foundation** (Phase 1-2) → Project compiles, backend serves routes
2. **MVP: Search** (Phase 3) → Users can search flights
3. **Fare Selection** (Phase 4) → Users can compare and select fares
4. **Booking Flow** (Phase 5-8) → Full booking with confirmation
5. **Localization** (Phase 9) → Arabic RTL support
6. **Offline** (Phase 10) → Saved bookings accessible offline
7. **Polish** (Phase 11-12) → Platform fixes, error handling

### Parallel Team Strategy

With multiple developers:

1. All complete Phase 1-2 together
2. Once Foundational done:
   - Developer A: US1 → US2 (search flow)
   - Developer B: US7 (localization - independent)
   - Developer C: US8 (offline - independent)
3. After US2:
   - Developer A: US3 → US4 → US5 → US6 (booking flow)
   - Developer B+C: Join booking flow or polish

---

## Summary

| Metric | Count |
|--------|-------|
| Total Tasks | 90 |
| Phase 1 (Setup) | 8 |
| Phase 2 (Foundational) | 35 |
| US1 (Search) | 11 |
| US2 (Fare Selection) | 4 |
| US3 (Passenger) | 4 |
| US4 (Ancillaries) | 2 |
| US5 (Payment) | 7 |
| US6 (Confirmation) | 3 |
| US7 (Language) | 4 |
| US8 (Offline) | 3 |
| Phase 11 (Platform) | 4 |
| Phase 12 (Polish) | 5 |
| Parallel Tasks [P] | 42 |

**Suggested MVP**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 54 tasks
**Full Feature**: All 90 tasks

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story checkpoint enables independent testing
- US7 (Language) and US8 (Offline) are independent of main booking flow
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
