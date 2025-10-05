package apps.sarafrika.elimika.commerce.medusa.service;

import java.util.LinkedHashMap;
import java.util.Map;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Higher level orchestrator that performs the full checkout flow on Medusa.
 */
@Service
public class MedusaOrderService {

    private final MedusaCartService cartService;

    public MedusaOrderService(MedusaCartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Completes the checkout flow by updating the cart details, selecting the payment session,
     * and finalising the cart to create an order.
     *
     * @param request checkout data collected from the Elimika platform
     * @return the resulting Medusa order representation
     */
    public MedusaOrderResponse completeCheckout(MedusaCheckoutRequest request) {
        if (request == null) {
            throw new MedusaIntegrationException("Checkout request must be provided");
        }

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("email", request.getCustomerEmail());
        if (StringUtils.hasText(request.getShippingAddressId())) {
            updates.put("shipping_address_id", request.getShippingAddressId());
        }
        if (StringUtils.hasText(request.getBillingAddressId())) {
            updates.put("billing_address_id", request.getBillingAddressId());
        }

        cartService.updateCart(request.getCartId(), updates);
        cartService.selectPaymentSession(request.getCartId(), request.getPaymentProviderId());
        return cartService.completeCart(request.getCartId());
    }
}
