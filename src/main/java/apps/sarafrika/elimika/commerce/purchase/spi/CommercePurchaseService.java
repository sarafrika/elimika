package apps.sarafrika.elimika.commerce.purchase.spi;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;

/**
 * Commerce Purchase Service Provider Interface
 * <p>
 * Provides order recording services for commerce module.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-24
 */
public interface CommercePurchaseService {

    /**
     * Records an order in the purchase module.
     *
     * @param order The order details from Medusa
     * @param checkoutRequest The checkout request details
     */
    void recordOrder(OrderResponse order, CheckoutRequest checkoutRequest);
}
