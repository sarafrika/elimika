package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * High level orchestration of order workflows for the commerce module.
 */
public interface OrderService {

    OrderResponse completeCheckout(CheckoutRequest request);

    OrderResponse getOrder(String orderId);

    /** For a verified signed receipt link only: the order with no caller check (the link is the proof). */
    OrderResponse getOrderForSignedLink(String orderId);

    /** The signed-in buyer's own orders, newest first. */
    Page<OrderResponse> listMyOrders(Pageable pageable);
}
