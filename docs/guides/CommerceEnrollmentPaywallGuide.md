# Commerce Checkout & Enrollment Paywall Guide

## 1. Overview

This guide walks frontend engineers and designers through the end-to-end experience of purchasing course/class access and enrolling students once payment is confirmed. It covers the required API calls, payload structure, and UI states you should support to keep the learner flow aligned with the backend paywall.

- **Cart API Root:** `/api/v1/commerce/carts`
- **Order API Root:** `/api/v1/commerce/orders`
- **Enrollment API Root:** `/api/v1/enrollment`
- **Error Code For Unpaid Access:** `402 Payment Required`

---

## 2. Experience Flow

1. Create cart (`POST /commerce/carts`) with `currency_code` and optional `region_code`.
2. Add items (`POST /commerce/carts/{id}/items`) with variant and course/class/student metadata.
3. Select payment provider (`POST /commerce/carts/{id}/payment-session`).
4. Checkout (`POST /commerce/orders/checkout`).
5. On payment success, attempt enrollment; paywall returns `402` if unpaid.

---

## 3. API Call Sequence

Sequence (text):
- UI → Cart API: create cart; receive `id`, totals.
- UI → Cart API: add items (variant + metadata) per selection; receive updated cart.
- UI → Cart API: select payment provider.
- UI → Order API: checkout with cart id, customer email, provider; wait for paid status.
- UI → Enrollment API: enroll class/student; handle `402` for unpaid.

---

## 4. Order Placement Sequence (Detailed)

Use the following ordered steps in your UI to ensure cart and checkout interactions stay aligned with backend expectations:

1. **Bootstrap Cart**  
   `POST /api/v1/commerce/carts` with `currency_code` (and optional `region_code`) to receive a `cart.id`.
2. **Add Line Items**  
   For each course/class selection call `POST /api/v1/commerce/carts/{cartId}/items` supplying the metadata contract below.
3. **Review / Edit Cart** *(optional)*  
   Fetch the cart via `GET /api/v1/commerce/carts/{cartId}` if you need to render a confirmation page or allow removals.
4. **Lock Payment Provider**  
   `POST /api/v1/commerce/carts/{cartId}/payment-session` with `provider_id` to prepare the payment session.
5. **Collect Payer Details**  
   Gather email (and address IDs if relevant) for the final checkout payload.
6. **Complete Checkout**  
   `POST /api/v1/commerce/orders/checkout` with the cart ID, payer email, and provider. Wait for `payment_status` to be `captured` or `paid` before moving forward.
7. **Display Order Summary**  
   Show the order identifier/number from the response and surface a “Continue to enrollment” CTA.
8. **Attempt Enrollment**  
   Call `POST /api/v1/enrollment` with `class_definition_uuid` and `student_uuid` only after confirming payment. The backend will enroll the learner into every scheduled instance for that class. Handle `402` responses as described later in this guide.

---

## 5. Cart Line Item Metadata Contract

Each cart line item represents a prepaid entitlement for a specific learner to access a course (and optionally a scheduled class). The identifiers you pass tell the backend exactly which records should be unlocked:

- **Course UUID** – Primary key from the Elimika course catalog (`courses` table). One course may have multiple classes; paying it typically unlocks all course resources.
- **Class Definition UUID** – Identifier for a scheduled class template (`class_definitions` table). It maps to cohorts or recurring sessions that the student will attend.
- **Student UUID** – Primary key for the learner in the `students` table (not the user UUID). Supports scenarios where a guardian purchases on behalf of a dependent.
- **Variant ID** – Internal commerce variant code/UUID representing the SKU for this course/class access.

### Fetching Catalog Metadata

Use the dedicated catalog API to retrieve the internal product/variant for a course or class before building the cart payload:

- `GET /api/v1/commerce/catalog/by-course/{courseUuid}` – returns the variant identifiers for a course-level purchase.
- `GET /api/v1/commerce/catalog/by-class/{classDefinitionUuid}` – returns the variant tied to a specific class definition.

Catalog items are managed via `POST/PUT /api/v1/commerce/catalog` by operations/admin tooling, ensuring the storefront can simply query and reuse the stored mapping.

Every cart line item must include metadata so the backend can tie a payment to course/class access. Missing keys will cause the paywall to block enrollment even after checkout.

| Key                     | Type    | Required | Description                                                                                                                                           |
|-------------------------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `course_uuid`           | UUID    | Yes      | Course granting access. Obtain this from the Course Catalogue API / course detail view that powers the storefront.                                   |
| `class_definition_uuid` | UUID    | Yes      | Class definition selected during scheduling. Fetch via the Class Definitions API (`GET /api/v1/classes/…`) when rendering available cohorts/sessions. |
| `student_uuid`          | UUID    | Yes      | Student who will consume the access (supports parents/admins purchasing for others). Read from the learner profile context or student selection UI.  |
| `variant_id`            | String  | Yes      | Internal commerce variant identifier representing the SKU for this product. Retrieve via the catalog endpoints above.                                |

**Example Payload When Adding A Line Item**

Before calling this endpoint you should already have:

1. Looked up the internal product/variant that represents the course seat via the catalog endpoints or preloaded catalog.
2. Pulled the `course_uuid` and `class_definition_uuid` from the course/class APIs as the learner chooses an offering.
3. Selected the `student_uuid` (current user or dependent) from the tenant’s student directory.

```json
{
  "variant_id": "variant_01HZX1Y4K8R0HVWZ4Q6CF6M1AP",
  "quantity": 1,
  "metadata": {
    "course_uuid": "5f5e0f54-59bb-4c77-b21d-6d496dd1b4b2",
    "class_definition_uuid": "0f6b8eaa-1f22-4a1b-9a3e-37cf582f58b7",
    "student_uuid": "8f4544d3-1741-47ba-aacc-5c9e0fbcd410"
  }
}
```

---

## 6. Checkout Steps & UI Checklist

1. **Create Cart** – Call `POST /commerce/carts` when the learner begins checkout. Store `cart.id`.
2. **Add Items** – Each course/class combination becomes an item with the metadata above.
3. **Review Cart** – Render cart items from the response and allow removal or quantity changes (if supported).
4. **Select Payment Provider** – `POST /commerce/carts/{cartId}/payment-session` uses the provider (e.g., `manual`, `mpesa`).
5. **Collect Payer Details** – Capture `customer_email` (and addresses if applicable).
6. **Complete Checkout** – `POST /commerce/orders/checkout` finalizes the order. Inspect `payment_status` (`captured`/`paid`) to mark success.
7. **Store Order Reference** – Use `order.id` or `order.display_id` for receipts and reconciliation.
8. **Advance to Enrollment UX** – Present “Continue to enrollment” CTA after payment.
9. **Email Receipt** – The backend now emits an order receipt email to the purchaser using the payment metadata and order totals; surface the same summary in the UI for consistency.

---

## 7. Handling Paywall Errors (HTTP 402)

When the frontend calls `POST /enrollment`, the backend verifies payment. If there is no matching paid purchase, you will receive:

```json
{
  "success": false,
  "message": "Class fee must be settled before enrollment is permitted.",
  "error": {
    "timestamp": "2025-10-10T05:40:00Z"
  }
}
```

### Age-gated enrollments
- After payment succeeds but before the class is reserved, the enrollment API checks the course/class age limits via `AgeVerificationService`. If the student’s DOB is missing or outside the allowed band, the endpoint returns `422` with an `AgeRestrictionException` message (e.g., “Student age 12 is below the minimum age 16 required for course ‘Algebra Basics’”).
- Require learners or guardians to supply DOB before checkout, store it in the student profile, and surface the backend message verbatim so families understand why the enrollment cannot proceed. Offer alternate classes or refund paths when appropriate.

### UI Guidance

- **Primary CTA:** “Complete payment to enroll.”
- **Secondary CTA:** “Review Cart & Payment History” linking to the order summary UI.
- **Contextual messaging:** show course/class name and student name involved.

---

## 8. Enrollment UI States

| State                 | Trigger                                               | Suggested UI Treatment                                                |
|-----------------------|-------------------------------------------------------|------------------------------------------------------------------------|
| Pending Payment       | 402 response from paywall                             | Display paywall card with CTA to reopen checkout.                     |
| Payment in Progress   | Checkout request sent, waiting on provider confirmation | Disable enrollment button, show spinner & “Verifying payment…” copy. |
| Enrollment Confirmed  | Enrollment API success                                | Show success toast + redirect to class dashboard/schedule.            |
| Enrollment Conflict   | 400/409 from scheduling (capacity or double-booking)  | Reuse existing scheduling conflict patterns.                          |

---

## 9. Reference API Payloads

### Create Cart

```http
POST /api/v1/commerce/carts
Content-Type: application/json
{
  "currency_code": "USD",
  "region_code": "KE",
  "items": []
}
```

### Checkout

```http
POST /api/v1/commerce/orders/checkout
Content-Type: application/json
{
  "cart_id": "2f6d4d1e-5f2a-4b2e-9f8d-0b7c3e9b5c1a",
  "customer_email": "learner@example.com",
  "payment_provider_id": "manual"
}
```

### Enrollment Attempt

```http
POST /api/v1/enrollment
Content-Type: application/json
{
  "class_definition_uuid": "0a39e4fa-0e97-4fd2-94ab-09bd4a67c676",
  "student_uuid": "8f4544d3-1741-47ba-aacc-5c9e0fbcd410"
}
```

---

## 10. Design Considerations

- **Cart Summary Card:** include course title, class schedule, learner name, and price so the paywall can reference it later.
- **Order Receipt View:** show payment status badge (`Paid`, `Pending`, `Failed`). Provide “Enroll Now” CTA only when status is a success state.
- **Error State Illustration:** differentiate paywall failures from scheduling conflicts to guide the learner appropriately.
- **Mobile Flow:** ensure cart and enrollment steps can be completed in separate sessions; persistence of `cart_id` and `order_id` is crucial.

---

## 11. QA Checklist

- [ ] Line item metadata is present for every course/class added to the cart.
- [ ] Payment status from `OrderResponse` is displayed in the UI receipt.
- [ ] Enrollment attempts after payment succeed without manual refresh.
- [ ] Unpaid enrollment attempts render the 402 paywall state.
- [ ] Parents/admins purchasing on behalf of a student use the correct `student_uuid`.

Following this guide keeps the user journey consistent with the backend paywall and ensures learners only access classes once their payments are confirmed.
