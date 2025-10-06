package apps.sarafrika.elimika.commerce.medusa.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaCartService;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaCustomerService;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaOrderService;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MedusaOrderServiceImpl implements MedusaOrderService {

    private final MedusaCartService cartService;
    private final MedusaCustomerService customerService;

    public MedusaOrderServiceImpl(MedusaCartService cartService, MedusaCustomerService customerService) {
        this.cartService = cartService;
        this.customerService = customerService;
    }

    @Override
    public MedusaOrderResponse completeCheckout(MedusaCheckoutRequest request) {
        if (request == null) {
            throw new MedusaIntegrationException("Checkout request must be provided");
        }

        MedusaCustomerResponse customer = customerService.ensureCustomer(MedusaCustomerRequest.builder()
                .email(request.getCustomerEmail())
                .build());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("email", request.getCustomerEmail());
        updates.put("customer_id", customer.getId());
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
