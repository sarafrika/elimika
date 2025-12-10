# Frontend Guide: Cart & Checkout (MPesa)

This guide explains how to implement cart and checkout flows against Elimika’s v1 commerce APIs using the internal commerce stack. The only supported payment provider today is MPesa (`provider_id = "mpesa"`).

## API Roots
- Cart: `/api/v1/commerce/carts`
- Orders: `/api/v1/commerce/orders`
- Enrollment (post-payment): `/api/v1/enrollment`

## End-to-End Flow (text)
1) Create a cart with `currency_code` (and optional `region_code`).
2) Add line items for each class/course selection; the backend attaches read-only catalogue metadata (course/class context) automatically.
3) Select payment provider (`mpesa`).
4) Complete checkout with cart id, customer email, and `payment_provider_id = "mpesa"`.
5) After payment is confirmed (`payment_status` paid/captured/authorized), proceed to enrollment; if unpaid, backend returns 402.

## Request/Response Contracts

### Create Cart
`POST /api/v1/commerce/carts`
```json
{
  "currency_code": "KES",
  "region_code": "KE",
  "items": []
}
```
Response (excerpt):
```json
{
  "id": "c3c4c7a1-0f18-46a0-99eb-6c0af0c2d1c4",
  "currency_code": "KES",
  "region_code": "KE",
  "status": "OPEN",
  "subtotal": "0.0000",
  "total": "0.0000",
  "items": []
}
```

### Add Item
`POST /api/v1/commerce/carts/{cartId}/items`
```json
{
  "variant_id": "class-definition-uuid",
  "quantity": 1
}
```
Response returns updated cart with totals and items (unit/subtotal/total are 4dp decimals). Metadata in responses is system-populated for catalogue context only.

### Select Payment Provider
`POST /api/v1/commerce/carts/{cartId}/payment-session`
```json
{
  "provider_id": "mpesa"
}
```

### Checkout
`POST /api/v1/commerce/orders/checkout`
```json
{
  "cart_id": "c3c4c7a1-0f18-46a0-99eb-6c0af0c2d1c4",
  "customer_email": "learner@example.com",
  "payment_provider_id": "mpesa"
}
```
Response (excerpt):
```json
{
  "id": "a1b2c3d4-1234-5678-9abc-def012345678",
  "payment_status": "PAID",
  "currency_code": "KES",
  "subtotal": "2500.0000",
  "total": "2500.0000",
  "items": [
    {
      "id": "f1e2d3c4-5678-1234-9abc-def012345678",
      "title": "Advanced Excel",
      "quantity": 1,
      "variant_id": "class-definition-uuid",
      "unit_price": "2500.0000",
      "subtotal": "2500.0000",
      "total": "2500.0000",
      "metadata": {
        "product_uuid": "product-uuid",
        "product_title": "Advanced Excel Course",
        "course_uuid": "course-uuid",
        "class_definition_uuid": "class-definition-uuid",
        "variant_uuid": "variant-uuid",
        "variant_code": "class-definition-uuid",
        "variant_title": "Advanced Excel"
      }
    }
  ]
}
```

### Enrollment (after payment)
`POST /api/v1/enrollment`
```json
{
  "class_definition_uuid": "class-definition-uuid",
  "student_uuid": "student-uuid"
}
```
- Success: enrollment DTOs.
- Payment missing/invalid: HTTP 402 with message “Payment required before enrollment is permitted”.

## UI State Checklist
- Show cart totals in currency (4dp), not cents.
- Block checkout unless `payment_provider_id` = `mpesa`.
- After checkout, poll or wait on the response; proceed only if `payment_status` in `PAID/CAPTURED/AUTHORIZED/PARTIALLY_CAPTURED`.
- On `402` from enrollment, display a “Payment required” state and send users back to checkout/order history.

## Data You Must Persist Client-Side
- `cart.id`
- `order.id` (and `order.display_id` if shown to users)
- Selected `payment_provider_id` (only `mpesa` currently)

## Error Handling Patterns
- 400: validation issues (missing currency_code, invalid UUIDs).
- 402: unpaid access when enrolling.
- 404: cart/order/enrollment targets not found.
- 409: duplicate enrollment or cart conflicts.

## Testing Checklist (frontend)
- Create cart → add item → select `mpesa` → checkout → enrollment success.
- Enrollment attempt without payment returns 402.
- Mismatched currency between cart and variants results in 400.
