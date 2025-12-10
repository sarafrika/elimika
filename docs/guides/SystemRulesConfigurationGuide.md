# System Rules Configuration Guide

This guide explains how to define and operate platform rules via the `/api/v1/system-rules` admin API. It covers payload shapes, scoping, and how rules flow from the UI to storage and into runtime evaluation.

## UI ↔ API ↔ Storage Flow
```
[Admin UI: System Rule form]
    ↓ POST/PUT /api/v1/system-rules (SystemRuleAdminController)
[SystemRuleAdminService → system_rules table (value_payload JSON)]
    ↓ RuleEvaluationService at runtime
[Feature modules]
  - Commerce checkout → PLATFORM_FEE
  - Onboarding → AGE_GATE
  - Future guards → ENROLLMENT_GUARD/CUSTOM
```

## Rule Categories and Scoping
- `PLATFORM_FEE`: Calculates platform fees during checkout.
- `AGE_GATE`: Enforces age/location/demographic constraints.
- `ENROLLMENT_GUARD` / `CUSTOM`: Reserved for future guards; payload is free-form JSON interpreted by the consumer.

Each rule can be scoped:
- `GLOBAL` (no `scope_reference`)
- `TENANT` (e.g., tenant UUID in `scope_reference`)
- `REGION` (ISO region/country code)
- `DEMOGRAPHIC` or `SEGMENT` (string identifiers)

Priority (higher first) and effective window (`effective_from`/`effective_to`) control which rule is selected when multiple match.

## Platform Fee Payload
Use `value_payload` with the following shape (mapped to `PlatformFeeConfig`):
```json
{
  "mode": "PERCENTAGE",          // or FLAT
  "rate": 2.5,                   // required if mode=PERCENTAGE (%)
  "amount": 0.00,                // required if mode=FLAT (major units)
  "currency": "USD",             // must be an active platform currency
  "waiver": {                    // optional: fully waive within window
    "percentage": 100,
    "start": "2025-01-01T00:00:00Z",
    "end": "2025-01-31T23:59:59Z"
  },
  "discount": {                  // optional: partial waiver within window
    "percentage": 50,
    "start": "2025-02-01T00:00:00Z",
    "end": "2025-02-07T23:59:59Z"
  }
}
```
Key points:
- `currency` must be active (platform currency validator rejects inactive/unknown codes).
- `mode=PERCENTAGE` expects `rate`; `mode=FLAT` expects `amount`.
- `waiver`/`discount` are time-bound; only one needs to be supplied.

Example rule creation:
```json
{
  "category": "PLATFORM_FEE",
  "key": "platform.fee.default",
  "scope": "GLOBAL",
  "status": "ACTIVE",
  "priority": 0,
  "value_type": "JSON",
  "value_payload": {
    "mode": "PERCENTAGE",
    "rate": 2.5,
    "currency": "USD"
  },
  "effective_from": "2025-01-01T00:00:00Z"
}
```

## Age Gate Payload
Use `value_payload` with the following shape (mapped to `AgeGateConfig`):
```json
{
  "minAge": 13,
  "maxAge": 18,
  "allowedRegions": ["KE", "UG"],
  "blockedRegions": [],
  "allowedDemographics": ["student", "guardian"],
  "blockedDemographics": []
}
```
Evaluation uses `RuleContext`:
- `ruleKey` (optional override),
- `tenantId`, `regionCode`,
- `demographicTags`/`segments`,
- `evaluationInstant` (defaults to current UTC).

## Creating Rules (API)
- Create/Update: `POST/PUT /api/v1/system-rules` with `SystemRuleRequest`
  - `category`: one of the categories above
  - `key`: unique within category
  - `scope` + `scope_reference` (if not global)
  - `status`: set to `ACTIVE` to apply
  - `priority`: integer (higher wins on tie)
  - `value_type`: typically `JSON`
  - `value_payload`: one of the payloads described above
  - `effective_from` / `effective_to`: window for rule validity
- Fetch/List: `GET /api/v1/system-rules/{uuid}` and `GET /api/v1/system-rules`

## Operational Guidance
- Keep a single active `PLATFORM_FEE` per scope; use `priority` and `effective_from/to` for scheduled changes.
- Ensure fee `currency` is aligned with active platform currencies; inactive/unknown codes are rejected.
- For age gates, prefer `allowed` lists and keep `blocked` narrow to avoid unexpected rejects.
- Document `key` naming in UI (e.g., `platform.fee.default`, `student.onboarding.age_gate`) to avoid collisions.

