# Master Implementation Plan: Project "Shadow" (fairair In-Housing)

Target Audience: AI Coding Assistant (Claude Code / Copilot)

Objective: Build a production-ready, full-stack Airline Booking Platform.

Architecture: Kotlin Multiplatform (Mobile/Web) + Quarkus (BFF Backend).

## 0. Global Constraints & Tech Stack

**Strictly adhere to these technology choices. Do not hallucinate alternative libraries.**

- **Monorepo Structure:** Gradle Multi-module.
    
- **Language:** Kotlin 2.0+ (Use K2 compiler).
    
- **Backend Framework:** Quarkus (RESTEasy Reactive, Jackson/Kotlinx.Serialization).
    
- **Frontend Framework:** Compose Multiplatform (Material 3).
    
- **Navigation:** Voyager.
    
- **Dependency Injection:** Koin (Annotations preferred).
    
- **Networking:** Ktor Client (Frontend) <-> RestEasy (Backend).
    
- **Shared Logic:** `commonMain` in a dedicated `:shared-contract` module.
    
- **Validation:** Use strict typing (Value Classes) where possible.
    

## Phase 1: Infrastructure & Scaffolding

**Goal:** Establish the project structure and build pipeline.

### Step 1.1: Project Initialization

1. Initialize a Gradle project with a Kotlin Multiplatform preset.
    
2. Configure three top-level modules:
    
    - `:shared-contract` (Kotlin Multiplatform - Pure Kotlin, no Android/JVM dependencies).
        
    - `:backend-quarkus` (Quarkus JVM application).
        
    - `:apps-kmp` (Compose Multiplatform for Android, iOS, and Wasm/Web).
        
3. Configure `libs.versions.toml` (Version Catalog) to manage dependencies centrally. Ensure distinct versions for Ktor (Client) and Quarkus (Server).
    

### Step 1.2: The "Shared Contract" Module

**Context:** This module is the source of truth. It must not depend on platform-specific libraries.

1. Create a folder structure for `dtos`, `api`, and `models`.
    
2. Define the `FlightSearchRequest` and `FlightResponse` data classes. Ensure they use `@Serializable`.
    
3. Define `Station` (Airport Code, Name) and `RouteMap` (Origin -> List<Destination>) models.
    
4. Define the `BookingRequest` (including Passenger details) and `BookingConfirmation` classes.
    
5. Create an `ApiRoutes` object holding constant string paths (e.g., `/v1/config/routes`, `/v1/search`, `/v1/booking`).
    
6. **Constraint:** All dates must be ISO-8601 strings or `kotlinx.datetime` types.
    

## Phase 2: The Backend (Quarkus BFF)

**Goal:** Create the middleware that translates "Clean API" to "Legacy Navitaire Logic."

### Step 2.1: Mock Data Layer

1. Create a resource folder `src/main/resources/mock-data`.
    
2. Place detailed JSON files here:
    
    - `navitaire_routes.json`: A list of valid Origin-Destination pairs (e.g., RUH->JED is valid, RUH->DXB is invalid).
        
    - `navitaire_availability.json`: Flight results.
        
    - `navitaire_pnr.json`: Booking details.
        
3. Implement a `NavitaireClient` interface.
    
4. Implement two versions of this interface:
    
    - `MockNavitaireClient`: Reads from the JSON files, adds a configurable artificial delay (0.5s - 1.5s) to simulate network latency.
        
    - `RealNavitaireClient`: (Skeleton only) Setup for Feign/RestClient to hit external URLs.
        
5. Add a configuration property `fairair.provider` to toggle between Mock and Real at runtime.
    

### Step 2.2: The Caching Service

1. Implement a service layer `FlightService` that sits between the Controller and the Client.
    
2. Integrate a Caching library (Caffeine).
    
3. **Logic:**
    
    - On `getRoutes()`: Cache the station list (TTL: 24 hours).
        
    - On `searchFlights(origin, dest, date)`: Check cache using a composite key.
        
    - If missing: Call `NavitaireClient`, transform the "Raw/Ugly" JSON into the clean `FlightResponse` DTO from the Shared Contract.
        
    - Store result in cache for 5 minutes.
        
    - Return result.
        

### Step 2.3: API Controllers

1. Create standard REST endpoints matching the `ApiRoutes` defined in Phase 1.
    
    - `/v1/config/routes`: Returns the valid station map to the frontend on app launch.
        
2. Ensure endpoints accept/return the specific DTOs from Phase 1.
    
3. Implement global error handling (transform downstream errors into clean 400/500 JSON responses).
    

## Phase 3: The Frontend (Compose Multiplatform)

**Goal:** A single UI codebase that runs on iOS, Android, and Web.

### Step 3.1: Design System & Theme

1. Create a custom Material 3 Theme (`FairairTheme`).
    
2. Define the Brand Colors: Purple (Primary), Lime Green (Secondary/Accent).
    
3. **Typography:** Setup support for both English (Latin) and Arabic (RTL support is critical). Implement a mechanism to switch App Language at runtime, triggering a full UI direction flip.
    

### Step 3.2: Architecture Skeleton

1. Setup **Koin** for dependency injection.
    
2. Setup **Voyager** for navigation. Create a `ScreenModel` (ViewModel) base class that handles "Loading," "Content," and "Error" states automatically.
    
3. Initialize **Ktor Client** with ContentNegotiation (JSON). Base URL should point to `localhost:8080` (for Dev) or the production URL.
    

### Step 3.3: Feature - Flight Search

1. **App Initialization:**
    
    - On app launch, fetch `/v1/config/routes`. Store this in a `StationRepository` singleton.
        
2. **Search Screen:**
    
    - **Logic:** When user selects "Origin" (e.g., JED), the "Destination" dropdown must update to show _only_ the valid connections defined in the Route Map.
        
    - Inputs: Origin/Dest (Airport selector), Date Picker, Passenger Counts.
        
    - Action: "Search Flights" button (Clickable once mandatory fields are filled).
        
3. **Results Screen:**
    
    - Fetch data from the Backend using the shared `FlightSearchRequest`.
        
    - **UI Component:** `FlightCard`. Should show departure/arrival times, duration, and price.
        
    - **UI Component:** `FareFamilySelector`. When a user clicks a flight, expand to show "Fly", "Fly+", "FlyMax" options with different perks.
        
    - Implement "Skeleton Loading" (Shimmer) while waiting for the API.
        

### Step 3.4: Feature - Passenger & Booking

1. **Passenger Form:**
    
    - Fields: Title, First Name, Last Name, Nationality, DOB, Document ID.
        
    - **Mobile Only:** Implement a placeholder button for "Scan Passport" (to be implemented with platform-specific CameraX/VisionKit later).
        
2. **Ancillaries (Simplified):**
    
    - Toggle for "Add Checked Bag (+100 SAR)".
        
3. **Payment Screen:**
    
    - Mock form for Credit Card entry.
        
    - "Pay Now" button sends `BookingRequest` to backend.
        
4. **Confirmation Screen:**
    
    - Display the PNR returned by the backend.
        
    - Button: "Save to Home" (Local storage).
        

## Phase 4: Platform Specifics & Polish

**Goal:** Make it feel Native, not like a website wrapper.

### Step 4.1: Local Persistence

1. Implement a local store (DataStore or SQLDelight) to save:
    
    - User Session Token.
        
    - Recent Search History.
        
    - Cached Boarding Passes (for Offline Mode).
        

### Step 4.2: Android Specifics

1. Configure `AndroidManifest.xml` for Internet permissions.
    
2. Implement the "Back Handler" to integrate Voyager with the Android system back button.
    

### Step 4.3: iOS Specifics

1. Configure the `MainViewController.kt` entry point.
    
2. Ensure Safe Area insets are handled in Compose (WindowInsets).
    

### Step 4.4: Web Specifics (Wasm)

1. Ensure the Ktor client uses the JS/Wasm engine.
    
2. Handle CORS issues in the Quarkus backend (allow `localhost:8080` and the web port).
    

## Phase 5: Testing & Validation

1. **Unit Tests (Shared):** Write tests for the `FlightRepository` mocking the HTTP client.
    
2. **Integration Tests (Backend):** Write tests ensuring the Mock Navitaire Client returns expected JSON structure.
    
3. **UI Tests:** Simple smoke test to ensure the App launches and navigates to Search.
    

**Execution Note:** When generating code, prioritize clean architecture. Do not put business logic inside Composable UI functions. Move logic to `ScreenModels`.