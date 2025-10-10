package apps.sarafrika.elimika.commerce.purchase.service;

import apps.sarafrika.elimika.commerce.order.dto.CheckoutRequest;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;

public interface CommercePurchaseService {

    void recordOrder(OrderResponse order, CheckoutRequest checkoutRequest);
}
