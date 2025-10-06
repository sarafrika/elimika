package apps.sarafrika.elimika.commerce.medusa.service;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;

/**
 * Higher level orchestrator that performs the full checkout flow on Medusa.
 */
public interface MedusaOrderService {

    /**
     * Completes the checkout flow by updating the cart details, selecting the payment session,
     * and finalising the cart to create an order.
     *
     * @param request checkout data collected from the Elimika platform
     * @return the resulting Medusa order representation
     */
    MedusaOrderResponse completeCheckout(MedusaCheckoutRequest request);

    /**
     * Retrieves a previously created order from Medusa using its identifier.
     *
     * @param orderId the Medusa order identifier
     * @return the Medusa order details
     */
    MedusaOrderResponse retrieveOrder(String orderId);
}
