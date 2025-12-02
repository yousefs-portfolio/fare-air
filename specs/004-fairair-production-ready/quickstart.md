# Quickstart: FairAir Production Ready

**Feature**: 004-fairair-production-ready
**Date**: 2025-11-29

## Prerequisites

- JDK 17+
- Android Studio (for Android development)
- Xcode 15+ (for iOS development)
- Node.js 18+ (for web development)

## Setup

### 1. Clone and Checkout

```bash
git clone <repository-url>
cd fairair
git checkout 004-fairair-production-ready
```

### 2. Start Backend

```bash
./gradlew :backend-spring:bootRun
```

Backend will be available at `http://localhost:8080`

### 3. Run Frontend

**Android:**
```bash
./gradlew :apps-kmp:installDebug
```

**iOS:**
```bash
cd apps-kmp
./gradlew :apps-kmp:iosSimulatorArm64Test
# Or open in Xcode: open iosApp/iosApp.xcworkspace
```

**Web (Wasm):**
```bash
./gradlew :apps-kmp:wasmJsBrowserDevelopmentRun
```
Web app available at `http://localhost:8081`

## Development Workflow

### Brand Changes (fairair → FairAir)

1. **Text References**: Update all string literals
   - `apps-kmp/src/commonMain/kotlin/com/fairair/app/localization/Strings.kt`
   - `apps-kmp/src/wasmJsMain/resources/index.html`

2. **Color Scheme**: Update theme colors
   - `apps-kmp/src/commonMain/kotlin/com/fairair/app/ui/theme/VelocityColors.kt`

   New palette:
   ```kotlin
   val Primary = Color(0xFF0D9488)      // Teal
   val Accent = Color(0xFFF97316)       // Coral
   val BackgroundDeep = Color(0xFF1E293B) // Dark Slate
   ```

3. **Package Names**: Consider renaming if required
   - `com.fairair` → `com.fairair`

### Stub Audit

Run audit to find all stubs/TODOs:
```bash
grep -rn "TODO\|FIXME\|stub\|placeholder\|workaround" \
  --include="*.kt" --include="*.xml" \
  apps-kmp/ backend-spring/ shared-contract/
```

### Testing

**Run All Tests:**
```bash
./gradlew test
```

**Run Specific Module:**
```bash
./gradlew :backend-spring:test
./gradlew :apps-kmp:jvmTest
```

## Key Files to Modify

### Theme/Branding
| File | Purpose |
|------|---------|
| `VelocityColors.kt` | Color definitions |
| `VelocityTheme.kt` | Theme composition |
| `Strings.kt` | App name, UI text |
| `index.html` | Web page title, loading screen |

### Booking Flow (New Screens)
| File | Purpose |
|------|---------|
| `PassengerDetailsScreen.kt` | Collect passenger info (NEW) |
| `BookingConfirmationScreen.kt` | Show booking success (NEW) |
| `BookingScreenModel.kt` | Booking flow state (NEW) |

### Localization
| File | Purpose |
|------|---------|
| `Strings.kt` | English + Arabic strings |
| `LocalizationProvider.kt` | Language switching logic |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/config/stations | List airports |
| GET | /api/v1/config/routes?origin=XXX | Routes from origin |
| POST | /api/v1/search | Search flights |
| POST | /api/v1/booking | Create booking |
| GET | /api/v1/booking/{code} | Get booking |
| GET | /api/v1/health | Health check |

## Verification Checklist

- [ ] App displays "FairAir" on all screens
- [ ] Teal/coral color scheme applied
- [ ] No "fairair" text references remain
- [ ] Search flow works end-to-end
- [ ] Booking confirmation displays
- [ ] Arabic language works with RTL
- [ ] Web (Wasm) loads without errors
- [ ] No TODO/FIXME markers in code
