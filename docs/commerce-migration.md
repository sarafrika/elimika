# Internal Commerce Migration Plan

Elimika will replace Medusa with an in-house commerce stack that covers catalog, carts, checkout, and orders. This document captures the work stages, safety levers, and data model changes to guide implementation and rollout.

## Feature Flag
- `commerce.internal.enabled` (default `false`): gate all new catalog/cart/order code paths until lower environments validate parity.
- Exposure: set via env var `COMMERCE_INTERNAL_ENABLED` or application configuration.

## Rollout Stages
1) **Foundation**: add internal database schema (catalog, variants, carts, orders, payments) and property binding for the feature flag. Leave Medusa paths untouched.
2) **Domain Services (flagged)**: implement internal catalog lookup, cart lifecycle, checkout/order state machine, and payment provider abstraction behind the flag.
3) **API Layer (flagged)**: expose `/api/v1/commerce/catalog|carts|orders` backed by internal services while preserving Medusa endpoints for fallback.
4) **Integration**: switch enrollment/paywall flows to internal endpoints when flag is on; keep activity feed/audit publishing unchanged.
5) **Cutover**: toggle flag in non-prod, monitor metrics/logs, then enable in production once carts/checkout/orders pass validation. Add migration for any initial catalog seed data if needed.
6) **Cleanup**: remove Medusa configuration/clients/DTOs and drop Medusa-specific tables/columns once stable.

## Data Model (new tables)
- `commerce_product`, `commerce_product_variant`: catalog and pricing/inventory metadata linked to courses/classes.
- `commerce_cart`, `commerce_cart_item`: cart lifecycle with totals, discounts, and tax/shipping placeholders.
- `commerce_order`, `commerce_order_item`: order snapshots with status/payment/fulfillment states.
- `commerce_payment`: payment attempts and external references per order.

## Open Tasks
- Map enrollment/paywall flows to the internal cart/checkout stack under the flag.
- Define payment provider plug points and provider-level configuration.
- Update guides (e.g., `docs/guides/CommerceEnrollmentPaywallGuide.md`) to remove Medusa references and include the new API contracts plus a Mermaid flow diagram.
