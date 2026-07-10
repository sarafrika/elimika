package apps.sarafrika.elimika.commerce.internal.service;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import java.util.Optional;

/**
 * Internal order orchestration backed by the in-house commerce stack.
 */
public interface InternalOrderService {

    OrderResponse completeCheckout(CheckoutRequest request);

    OrderResponse getOrder(String orderId);

    /**
     * Marks an order's payment as captured (idempotent) and returns the refreshed order.
     * Used by the dev auto-capture path and by the M-Pesa confirmation callback.
     */
    OrderResponse markOrderCaptured(String orderId);

    /**
     * Persists the M-Pesa STK Push checkout request id against the order so payment status
     * can later be polled from the gateway.
     */
    void storeCheckoutRequestId(String orderId, String checkoutRequestId);

    /**
     * Returns the M-Pesa checkout request id previously stored against the order, if any.
     */
    Optional<String> findCheckoutRequestId(String orderId);
}
