# Research: FairAir Production Ready

**Feature**: 004-fairair-production-ready
**Date**: 2025-11-29

## Research Topics

### 1. Brand Color Scheme Selection

**Decision**: Teal (#0D9488) as primary, Coral (#F97316) as accent, Dark Slate (#1E293B) for backgrounds

**Rationale**:
- Teal conveys trust, reliability, and professionalism - key airline brand values
- Coral provides warm, inviting accent that contrasts well with teal
- Similar energy to the original purple/yellow but with a distinct identity
- Both colors maintain high accessibility contrast ratios
- Color palette aligns with modern airline industry trends (similar to WestJet, Aer Lingus)

**Alternatives Considered**:
- Blue (#2563EB) / Orange (#EA580C): Too common in airline industry, less distinctive
- Green (#059669) / Gold (#D97706): Good but green may conflict with "eco" expectations
- Navy (#1E3A5A) / Amber (#F59E0B): More corporate, less approachable

### 2. Stub/TODO Audit Approach

**Decision**: Systematic grep-based audit followed by categorized remediation

**Rationale**:
- Comprehensive search for `TODO`, `FIXME`, `stub`, `placeholder`, `workaround`
- Categorize findings by severity (blocking vs. nice-to-have)
- Replace stubs with production implementations
- Document any intentional TODOs (technical debt) with justification

**Audit Commands**:
```bash
# Find all TODO/FIXME/stub markers
grep -rn "TODO\|FIXME\|stub\|placeholder\|workaround" --include="*.kt" --include="*.xml"
```

### 3. Cross-Platform Parity Strategy

**Decision**: Use shared commonMain code for all business logic, platform-specific only for truly unavoidable cases

**Rationale**:
- Compose Multiplatform already provides 95%+ code sharing
- Platform differences should only exist for:
  - Entry points (MainActivity, SwiftUI App, main.kt for Wasm)
  - Platform-specific APIs (file system, notifications)
- Voyager Navigator issue on Wasm requires WasmApp.kt alternative

**Platform-Specific Requirements**:
| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Navigation | Voyager | Voyager | State-based (WasmApp) |
| Settings Storage | SharedPreferences | UserDefaults | localStorage |
| Network | OkHttp | Darwin | Ktor-JS |

### 4. Booking Flow Completion

**Decision**: Implement full flow: Search → Results → Fare Selection → Passenger Details → Confirmation

**Current State Analysis**:
- Search screen: ✅ Complete
- Results screen: ✅ Complete (VelocityResultsScreen)
- Fare selection: ✅ Complete (FareFamily selection)
- Passenger details: ⚠️ Needs implementation
- Booking confirmation: ⚠️ Needs implementation

**Missing Components**:
1. PassengerDetailsScreen - collect name, email, phone
2. BookingConfirmationScreen - show reference number, flight summary
3. BookingService integration - connect UI to backend booking endpoint

### 5. Arabic/RTL Implementation Verification

**Decision**: Validate existing RTL implementation, fill gaps in localization strings

**Current State**:
- LocalizationProvider: ✅ Exists
- Strings.kt: ⚠️ Needs completion for all UI strings
- RTL layout switching: ✅ Working via CompositionLocalProvider
- Arabic fonts: ✅ Noto Kufi Arabic included

**Missing Strings**:
- Passenger details labels
- Booking confirmation messages
- Error messages (all scenarios)

### 6. Error Handling Audit

**Decision**: Ensure all error paths show user-friendly messages

**Current Implementation**:
- ErrorMessageMapper: ✅ Exists
- Network errors: ✅ Handled with retry
- API errors: ⚠️ Need to verify all endpoints

**Best Practices**:
- Never expose technical details (stack traces, error codes) to users
- Always provide actionable guidance ("Try again", "Check connection")
- Log technical details server-side for debugging

## Summary of Findings

| Area | Status | Action Required |
|------|--------|----------------|
| Brand colors | Resolved | Implement teal/coral palette |
| Stub audit | Resolved | Run grep audit, remediate all findings |
| Cross-platform | Resolved | Maintain WasmApp.kt for web |
| Booking flow | Partial | Implement passenger details + confirmation |
| Arabic/RTL | Partial | Complete localization strings |
| Error handling | Partial | Verify all error paths |

## Next Steps

Proceed to Phase 1: Data Model and API Contracts
