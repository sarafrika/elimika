package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.commerce.order.dto.CheckoutRequest;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;

/**
 * High level orchestration of order workflows for the commerce module.
 */
public interface OrderService {

    OrderResponse completeCheckout(CheckoutRequest request);

    OrderResponse getOrder(String orderId);
}
