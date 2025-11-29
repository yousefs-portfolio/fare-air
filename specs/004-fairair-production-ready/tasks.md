# Tasks: FairAir Production Ready

**Input**: Design documents from `/specs/004-fairair-production-ready/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Audit & Preparation)

**Purpose**: Audit existing codebase and prepare for rebrand

- [ ] T001 Run stub/TODO audit: `grep -rn "TODO\|FIXME\|stub\|placeholder\|workaround" --include="*.kt" apps-kmp/ backend-spring/ shared-contract/`
- [ ] T002 [P] Document all flyadeal references: `grep -rn "flyadeal\|Flyadeal\|FLYADEAL" --include="*.kt" --include="*.xml" --include="*.html" .`
- [ ] T003 [P] Verify existing booking endpoint works in backend-spring/src/main/kotlin/com/flyadeal/controller/BookingController.kt

---

## Phase 2: Foundational (Brand & Theme Infrastructure)

**Purpose**: Core brand changes that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until brand foundation is complete

- [ ] T004 Update VelocityColors.kt with FairAir palette (teal #0D9488, coral #F97316) in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityColors.kt
- [ ] T005 [P] Update VelocityTypography.kt color references in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTypography.kt
- [ ] T006 [P] Update VelocityTheme.kt gradient colors in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTheme.kt
- [ ] T007 [P] Update index.html background color and loading spinner in apps-kmp/src/wasmJsMain/resources/index.html
- [ ] T008 [P] Update app title to "FairAir" in index.html in apps-kmp/src/wasmJsMain/resources/index.html
- [ ] T009 Update Strings.kt appName to "FairAir" in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T010 [P] Update AndroidManifest.xml app_name in apps-kmp/src/androidMain/AndroidManifest.xml

**Checkpoint**: Brand infrastructure ready - color scheme and naming updated

---

## Phase 3: User Story 1 - Complete Flight Search and Booking Flow (Priority: P1) 🎯 MVP

**Goal**: Users can search flights, select fares, enter passenger details, and receive booking confirmation

**Independent Test**: Search RUH→JED, select flight, enter passenger "Test User", confirm booking, verify confirmation code displayed

### Implementation for User Story 1

- [ ] T011 [US1] Create PassengerDetailsScreen.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/PassengerDetailsScreen.kt
- [ ] T012 [P] [US1] Create PassengerDetailsState.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/PassengerDetailsState.kt
- [ ] T013 [P] [US1] Create PassengerForm composable in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/PassengerForm.kt
- [ ] T014 [US1] Create BookingConfirmationScreen.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingConfirmationScreen.kt
- [ ] T015 [P] [US1] Create BookingConfirmationState.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingConfirmationState.kt
- [ ] T016 [US1] Create BookingScreenModel.kt to manage booking flow state in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingScreenModel.kt
- [ ] T017 [US1] Update AppModule.kt to register BookingScreenModel in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/di/AppModule.kt
- [ ] T018 [US1] Add booking navigation to App.kt (Voyager screens) in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/App.kt
- [ ] T019 [US1] Add passenger detail strings to Strings.kt (firstName, lastName, email, phone, passengerType) in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T020 [US1] Add booking confirmation strings to Strings.kt (confirmationTitle, bookingReference, thankYou) in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T021 [US1] Update WasmApp.kt with booking flow navigation (state-based) in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/WasmApp.kt
- [ ] T022 [US1] Implement input validation in PassengerForm (name, email, phone format) in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/PassengerForm.kt
- [ ] T023 [US1] Connect BookingScreenModel to BookingController API in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingScreenModel.kt
- [ ] T024 [US1] Add error handling for booking failures with user-friendly messages in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingScreenModel.kt

**Checkpoint**: Complete booking flow functional - search through confirmation works end-to-end

---

## Phase 4: User Story 2 - Brand Experience with FairAir Identity (Priority: P1)

**Goal**: All screens display FairAir branding with no flyadeal references

**Independent Test**: Navigate all screens, verify "FairAir" appears, no "flyadeal" text visible

### Implementation for User Story 2

- [ ] T025 [P] [US2] Update heroTitle in VelocitySearchScreen.kt to "FairAir" in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt
- [ ] T026 [P] [US2] Update SearchScreen.kt header text in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchScreen.kt
- [ ] T027 [P] [US2] Update VelocityResultsScreen.kt header to FairAir in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsScreen.kt
- [ ] T028 [P] [US2] Update VelocitySettingsScreen.kt brand references in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/settings/VelocitySettingsScreen.kt
- [ ] T029 [P] [US2] Update WasmApp.kt loading/placeholder text in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/WasmApp.kt
- [ ] T030 [P] [US2] Update main.kt window title in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/main.kt
- [ ] T031 [P] [US2] Update ResultsHeader.kt brand display in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/ResultsHeader.kt
- [ ] T032 [US2] Global search and replace remaining flyadeal references in apps-kmp/
- [ ] T033 [P] [US2] Update backend application.yml app name in backend-spring/src/main/resources/application.yml
- [ ] T034 [US2] Verify zero flyadeal references: `grep -rn "flyadeal" --include="*.kt" apps-kmp/`

**Checkpoint**: Brand audit complete - FairAir branding consistent across all screens

---

## Phase 5: User Story 3 - Multi-Language Support (Priority: P2)

**Goal**: Complete English and Arabic translations with proper RTL support

**Independent Test**: Switch to Arabic in settings, navigate all screens, verify RTL layout and Arabic text

### Implementation for User Story 3

- [ ] T035 [P] [US3] Add missing Arabic strings for passenger details in Strings.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T036 [P] [US3] Add Arabic strings for booking confirmation in Strings.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T037 [P] [US3] Add Arabic strings for all error messages in Strings.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T038 [P] [US3] Verify RTL layout in PassengerDetailsScreen in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/PassengerDetailsScreen.kt
- [ ] T039 [P] [US3] Verify RTL layout in BookingConfirmationScreen in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/booking/BookingConfirmationScreen.kt
- [ ] T040 [US3] Test full booking flow in Arabic language
- [ ] T041 [US3] Verify language persistence across app restarts in LocalizationProvider

**Checkpoint**: Full Arabic support - all screens work in RTL with Arabic text

---

## Phase 6: User Story 4 - Cross-Platform Consistency (Priority: P2)

**Goal**: Android, iOS, and Web behave identically

**Independent Test**: Run same booking flow on all three platforms, verify identical results

### Implementation for User Story 4

- [ ] T042 [P] [US4] Verify PassengerDetailsScreen renders correctly on Android in apps-kmp/src/androidMain/
- [ ] T043 [P] [US4] Verify PassengerDetailsScreen renders correctly on iOS in apps-kmp/src/iosMain/
- [ ] T044 [P] [US4] Verify PassengerDetailsScreen renders correctly on Web in apps-kmp/src/wasmJsMain/
- [ ] T045 [US4] Add WasmPassengerDetailsScreen for Wasm-specific navigation in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/WasmApp.kt
- [ ] T046 [US4] Add WasmBookingConfirmationScreen for Wasm-specific navigation in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/WasmApp.kt
- [ ] T047 [US4] Test booking flow on Android emulator
- [ ] T048 [US4] Test booking flow on iOS simulator
- [ ] T049 [US4] Test booking flow on Web (localhost:8081)

**Checkpoint**: Cross-platform parity verified - identical experience on all platforms

---

## Phase 7: User Story 5 - Saved Bookings and Preferences (Priority: P3)

**Goal**: Users can view booking history

**Independent Test**: Complete booking, navigate to saved bookings, verify booking appears

### Implementation for User Story 5

- [ ] T050 [P] [US5] Update SavedBookingsScreen to use FairAir branding in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/bookings/SavedBookingsScreen.kt
- [ ] T051 [P] [US5] Add saved bookings strings for Arabic in Strings.kt in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt
- [ ] T052 [US5] Implement booking persistence using multiplatform-settings in apps-kmp/src/commonMain/kotlin/com/flyadeal/app/state/
- [ ] T053 [US5] Add saved bookings to WasmApp.kt navigation in apps-kmp/src/wasmJsMain/kotlin/com/flyadeal/app/WasmApp.kt
- [ ] T054 [US5] Test offline viewing of saved bookings

**Checkpoint**: Saved bookings feature complete

---

## Phase 8: Polish & Production Readiness

**Purpose**: Final cleanup and verification

- [ ] T055 Remove all TODO/FIXME comments from codebase
- [ ] T056 [P] Remove all stub implementations (verify only MockNavitaireClient remains)
- [ ] T057 [P] Verify error messages are user-friendly (no technical codes)
- [ ] T058 Update CLAUDE.md with feature completion notes
- [ ] T059 Run full build: `./gradlew build`
- [ ] T060 Run backend tests: `./gradlew :backend-spring:test`
- [ ] T061 Verify web app loads without errors on fresh browser
- [ ] T062 Final brand audit: `grep -rn "flyadeal" --include="*.kt" --include="*.xml" --include="*.html" .`

**Checkpoint**: Production ready - zero stubs, zero TODOs, FairAir branding complete

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 - BLOCKS all user stories
- **Phase 3 (US1)**: Depends on Phase 2 - Core booking flow
- **Phase 4 (US2)**: Depends on Phase 2 - Can run parallel with US1
- **Phase 5 (US3)**: Depends on Phase 3 (needs new screens to localize)
- **Phase 6 (US4)**: Depends on Phase 3 (needs new screens to test)
- **Phase 7 (US5)**: Depends on Phase 2 - Can run parallel with US1-4
- **Phase 8 (Polish)**: Depends on all user stories

### User Story Dependencies

- **US1 (P1)**: Foundation only - MVP
- **US2 (P1)**: Foundation only - Can parallel with US1
- **US3 (P2)**: Depends on US1 (new screens need strings)
- **US4 (P2)**: Depends on US1 (new screens need platform testing)
- **US5 (P3)**: Foundation only - Independent

### Parallel Opportunities

Within Phase 2 (Foundational):
- T004, T005, T006, T007, T008, T010 can all run in parallel

Within Phase 3 (US1):
- T012, T013, T015 can run in parallel (state files)
- T011, T014 depend on state files

Within Phase 4 (US2):
- T025-T031, T033 can all run in parallel

Within Phase 5-7:
- Many verification tasks can run in parallel

---

## Parallel Example: Phase 2 Foundation

```bash
# Launch all color/theme updates together:
Task: "Update VelocityColors.kt"
Task: "Update VelocityTypography.kt"
Task: "Update VelocityTheme.kt"
Task: "Update index.html"
```

## Parallel Example: User Story 1

```bash
# Launch state files together:
Task: "Create PassengerDetailsState.kt"
Task: "Create BookingConfirmationState.kt"
Task: "Create PassengerForm composable"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1: Setup audit
2. Complete Phase 2: Brand foundation
3. Complete Phase 3: US1 booking flow
4. Complete Phase 4: US2 brand verification
5. **STOP and VALIDATE**: Full booking works with FairAir branding
6. Deploy/demo MVP

### Incremental Delivery

1. Setup + Foundation → Brand ready
2. Add US1 → Booking works → MVP!
3. Add US2 → Brand complete
4. Add US3 → Arabic support
5. Add US4 → Platform verification
6. Add US5 → Saved bookings
7. Polish → Production ready

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Verify each phase checkpoint before proceeding
- Commit after each task or logical group
- MockNavitaireClient is the ONLY permitted mock
