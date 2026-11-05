package apps.sarafrika.elimika.commerce.internal.service;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

/**
 * Internal order orchestration backed by the in-house commerce stack.
 */
public interface InternalOrderService {

    OrderResponse completeCheckout(CheckoutRequest request);

    OrderResponse getOrder(String orderId);

    /** For a verified signed receipt link only: the order with no caller check (the link is the proof). */
    OrderResponse getOrderForSignedLink(String orderId);

    /** The caller's own orders, paid or not, newest first; empty for an anonymous caller. */
    Page<OrderResponse> listOrdersForCurrentUser(Pageable pageable);

    /**
     * Marks an order's payment as captured (idempotent) and returns the refreshed order.
     * Used by the dev auto-capture path and by the M-Pesa confirmation callback.
     */
    /** Captures the order; empty when another confirmation already captured it. */
    Optional<OrderResponse> markOrderCaptured(String orderId);

    /** As {@link #markOrderCaptured(String)}, recording the gateway receipt and the paying number. */
    Optional<OrderResponse> markOrderCaptured(String orderId, String providerReceipt, String payerMsisdn);

    /**
     * Persists the M-Pesa STK Push checkout request id against the order so payment status
     * can later be polled from the gateway.
     */
    void storeCheckoutRequestId(String orderId, String checkoutRequestId);

    /** As above, also keeping the number the prompt went to for the seat-released SMS. */
    void storeCheckoutRequestId(String orderId, String checkoutRequestId, String promptMsisdn);

    /**
     * Returns the M-Pesa checkout request id previously stored against the order, if any.
     */
    Optional<String> findCheckoutRequestId(String orderId);
}
