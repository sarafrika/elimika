package apps.sarafrika.elimika.shared.event.commerce;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;

/**
 * Published when an order has been completed so downstream modules can react (e.g., record purchases).
 */
public record OrderCompletedEvent(
        OrderResponse order,
        CheckoutRequest checkoutRequest
) {
}
