# Booking and Payments Guide

## 1. Overview

This guide explains how students book instructors for course sessions, how the platform holds availability, and how payments move the booking through its lifecycle. The booking service owns booking records, coordinates availability holds, and listens to payment callbacks.

**API Root:** `/api/v1/bookings`  
**Storage:** `bookings` table (+ availability blocks via `instructor_availability`)  
**Statuses:** `payment_required` → `confirmed` → `cancelled`/`payment_failed`/`expired`

---

## 2. UI ↔ API ↔ Storage Flow

```mermaid
flowchart LR
    UI[Course UI] -->|POST /api/v1/bookings\nstudent, course, instructor, start, end, price| BookingSvc[Booking Service]
    BookingSvc -->|Insert booking\nstatus=payment_required| DB[(bookings table)]
    BookingSvc -->|Block slot| Availability[instructor_availability]
    BookingSvc -->|Create payment session| Pay[Payment Engine]
    Pay -->|POST /api/v1/bookings/{uuid}/payment-callback\npayment_status, reference| BookingSvc
    BookingSvc -->|Update status\nconfirmed/payment_failed| DB
    BookingSvc -->|Keep or release block| Availability
    UI -->|GET /api/v1/bookings/{uuid}\nor notifications| BookingSvc

    subgraph Cleanup
      Cron[Job every 5 mins] -->|Expire holds past hold_expires_at| BookingSvc
      BookingSvc -->|status = expired\nrelease block| DB
    end
```

---

## 3. Endpoints

- `POST /api/v1/bookings` — Create booking, place an availability hold, start payment session.
- `GET /api/v1/bookings/{bookingUuid}` — Fetch booking details/status.
- `POST /api/v1/bookings/{bookingUuid}/cancel` — Cancel booking, release hold (if present).
- `POST /api/v1/bookings/{bookingUuid}/payment-callback` — Payment engine callback to confirm or fail the booking.

---

## 4. Request/Response Examples

### Create Booking
```http
POST /api/v1/bookings
Content-Type: application/json

{
  "student_uuid": "11111111-2222-3333-4444-555555555555",
  "course_uuid": "aaaa1111-bbbb-2222-cccc-333333333333",
  "instructor_uuid": "99999999-8888-7777-6666-555555555555",
  "start_time": "2025-02-15T09:00:00",
  "end_time": "2025-02-15T10:00:00",
  "price_amount": 50.00,
  "currency": "USD",
  "purpose": "1:1 coaching session"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Booking created",
  "data": {
    "uuid": "b1234567-89ab-cdef-0123-456789abcdef",
    "status": "payment_required",
    "payment_session_id": "sess_123",
    "availability_block_uuid": "c2345678-90ab-cdef-1234-567890abcdef",
    "hold_expires_at": "2025-02-15T08:30:00Z",
    "start_time": "2025-02-15T09:00:00",
    "end_time": "2025-02-15T10:00:00"
  }
}
```

### Payment Callback
```http
POST /api/v1/bookings/{bookingUuid}/payment-callback
Content-Type: application/json

{
  "payment_reference": "pi_12345",
  "payment_status": "succeeded",
  "payment_engine": "placeholder"
}
```
Updates booking to `confirmed`. Use `payment_status: failed` to mark `payment_failed` and release the hold.

### Cancel Booking
```http
POST /api/v1/bookings/{bookingUuid}/cancel
```
Sets status to `cancelled` and releases the availability block (if present).

---

## 5. Availability Interaction

- On create: service checks instructor availability, then blocks the slot via `POST /api/v1/instructors/{instructorUuid}/availability/block` (bulk JSON payload) and stores the `availability_block_uuid`.
- On payment failure/cancel/expiry: service calls `removeBlockedSlot` to release the hold.
- On confirmation: block remains, representing the confirmed session.

---

## 6. Holds and Expiry

- Default hold length: 30 minutes (or until session start, whichever is sooner).
- Scheduled job `BookingHoldCleanupJob` runs every 5 minutes to mark `payment_required` bookings as `expired` after `hold_expires_at` and releases the availability block.

---

## 7. Validation and Status Rules

- Booking requires `student_uuid`, `course_uuid`, `instructor_uuid`, `start_time`, `end_time`.
- Status transitions:
  - `payment_required` → `confirmed` (payment succeeded)
  - `payment_required` → `payment_failed` (payment failed) → hold released
  - `payment_required` → `cancelled` or `expired` → hold released
  - `confirmed` → `cancelled` (manual) → hold released if policy allows
- Price and currency are optional; payment engine can be swapped in later via `PaymentGatewayClient`.

---

## 8. Frontend Integration Tips

- Immediately redirect to payment checkout using the returned `payment_session_id` / engine URL.
- Poll `GET /api/v1/bookings/{uuid}` or subscribe to notifications to show status transitions.
- Surface hold expiry in UI using `hold_expires_at` to warn users before release.
- When showing instructor availability, filter out slots already blocked by confirmed bookings (or held bookings if you choose to display holds).
