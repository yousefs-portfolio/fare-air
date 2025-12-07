# FareAir AI Agent Demo - Technical Specification

## 1. Project Overview

Goal: Demonstrate a "Voice-First" airline experience for FareAir where users can search, book, and manage flights using natural language (English & Saudi Arabic).

Key Value: Accelerates complex flows (like changing a seat or splitting a booking) from 10+ clicks to 2 voice commands.

### Technology Stack

- **Frontend:** Kotlin Multiplatform (KMP) - Android & iOS (Compose Multiplatform).
    
- **Backend:** Spring Boot (Webflux) - Middleware & Orchestrator.
    
- **AI Provider:** Google Vertex AI (Claude 3.5 Sonnet/Haiku) for Demo. _Migration path to AWS Bedrock defined._
    
- **Mock System:** In-memory "Mock Navitaire" service to simulate FareAir airline logic.
    

## 2. Architecture & Data Flow

**Pattern:** Hexagonal Architecture (Ports & Adapters) to allow swapping AI providers.

1. **Client (KMP):** Captures Voice/Text -> Sends to Backend.
    
2. **Spring Backend:**
    
    - Maintains `ChatSession`.
        
    - Calls `GenAiProvider` (Vertex Implementation).
        
    - Executes `Tools` (Mock Navitaire).
        
    - Returns `AiResponse` + `UiPayload` (JSON).
        
3. **Client (KMP):** Renders text response AND Native UI Cards (Generative UI) based on `UiPayload`.
    

## 3. Frontend Specification (KMP)

### 3.1 Visual Identity

- **Theme Strategy:** Inherit strictly from the existing FareAir application theme configuration (e.g., `Theme.kt`, `Color.kt`, or `MaterialTheme`).
    
- **Colors:** Do not use hardcoded hex values. Use semantic color references (e.g., `MaterialTheme.colorScheme.primary`, `FareAirColors.BrandPrimary`).
    
- **Typography:** Use the existing app's typography and font configuration.
    
- **Layout Direction:** Dynamic (LTR for English, RTL for Arabic).
    

### 3.2 Core Components

1. **The "Faris" Orb (AI Assistant):**
    
    - Floating Action Button (FAB) overlay.
        
    - Animation: Pulses when listening.
        
    - Action: Opens half-height BottomSheet (Chat Overlay).
        
2. **Chat Stream (Polymorphic List):**
    
    - `TextBubble`: Standard chat text.
        
    - `FlightCarousel`: Horizontal scroll of flight options.
        
    - `SeatMapWidget`: Mini interactive seat map.
        
    - `BoardingPassCard`: QR Code rendering.
        
    - `ComparisonCard`: "Old Flight vs New Flight" with price difference.
        

### 3.3 Voice Logic

- **Input:** Use native `SpeechRecognizer`.
    
    - Locale: `en-US` or `ar-SA` (Crucial for dialect capture).
        
- **Output:** TTS (Text-to-Speech) reading the `text` response.
    

## 4. Backend Specification (Spring Webflux)

### 4.1 Data Models (DTOs)

**`ChatMessage`**

```
data class ChatMessage(
    val role: String, // "user", "assistant"
    val content: String,
    val toolCalls: List<ToolCall>? = null
)

```

**`AiResponse` (Returned to Client)**

```
data class AiResponse(
    val text: String,        // "I found 3 flights..."
    val uiType: String?,     // "FLIGHT_LIST", "SEAT_MAP", "BOARDING_PASS"
    val uiData: String?      // JSON String of the payload for the UI
)

```

### 4.2 Mock Navitaire Service (Crucial Logic)

**Feature: Split PNR (Partial Cancellation)**

- **Concept:** To cancel ONE person in a group, you must split them into a new PNR, then cancel that PNR.
    
- **Mock Logic:**
    
    1. Find Booking `ABC123`.
        
    2. Find Passenger `Sarah` in list.
        
    3. Remove `Sarah` from `ABC123`.
        
    4. Create new (cancelled) Booking `XYZ999` for `Sarah`.
        
    5. Return success message.
        

**Feature: Smart Search**

- **Logic:** If `origin` is missing, default to `RUH` (Riyadh).
    
- **Logic:** If `date` is "next Friday", calculate `LocalDate`.
    

## 5. AI Configuration ("The Brain")

### 5.1 System Prompt (Bilingual Persona)

**Role:** "Faris", FareAir's intelligent assistant.

**Instructions:**

1. **Language:** Detect user language.
    
    - **English:** Be concise, professional.
        
    - **Arabic:** Use **Saudi White Dialect** (Khaleeji). Use terms like "Abshir" (Sure), "Halla" (Welcome), "Sim" (Yes/Ok). **DO NOT use formal MSA (Fusha).**
        
2. **Tool Use:** ALWAYS use English values for tool arguments (e.g., City Codes `RUH`, `JED`).
    
3. **Behavior:**
    
    - If user says "Change seat", ask "Aisle or Window?".
        
    - If user says "Cancel Sarah", ask for confirmation first.
        
    - Always try to upsell a "FareAir Bundle" (Bag + Meal) if they add items separately.
        

### 5.2 Tool Definitions (JSON Schemas)

**1. `search_flights`**

```
{
  "name": "search_flights",
  "description": "Finds available flights.",
  "parameters": {
    "type": "object",
    "properties": {
      "origin": {"type": "string", "description": "IATA Code (e.g. RUH)"},
      "destination": {"type": "string", "description": "IATA Code (e.g. JED)"},
      "date": {"type": "string", "description": "YYYY-MM-DD"}
    },
    "required": ["origin", "destination"]
  }
}

```

**2. `cancel_specific_passenger` (The Edge Case)**

```
{
  "name": "cancel_specific_passenger",
  "description": "Cancels ONE specific person from a booking, leaving others active.",
  "parameters": {
    "type": "object",
    "properties": {
      "pnr": {"type": "string"},
      "passenger_name": {"type": "string", "description": "First name of passenger to remove"}
    },
    "required": ["pnr", "passenger_name"]
  }
}

```

**3. `calculate_change_fees`**

```
{
  "name": "calculate_change_fees",
  "description": "Returns the cost difference to move a booking to a new flight.",
  "parameters": {
    "type": "object",
    "properties": {
      "pnr": {"type": "string"},
      "new_flight_number": {"type": "string"}
    }
  }
}

```

## 6. Integration Contract (API)

**Endpoint:** `POST /api/chat/v1/message`

**Request:**

```
{
  "sessionId": "user_123_session",
  "message": "Abgha aghayir maqa3d Sarah" // "I want to change Sarah's seat"
}

```

**Response (Standard):**

```
{
  "text": "Abshir! Sarah is currently in 12B. I can move her to 12F (Window) or 12C (Aisle).",
  "uiType": "SEAT_MAP",
  "uiData": {
    "pnr": "ABC123",
    "highlightedRow": 12,
    "availableSeats": ["12F", "12C"]
  }
}

```

_Note: The frontend sees `uiType: "SEAT_MAP"` and triggers the visual Seat Map overlay immediately._