package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaOrderService;
import apps.sarafrika.elimika.commerce.order.dto.CheckoutRequest;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.commerce.shared.mapper.MedusaCommerceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default {@link OrderService} implementation delegating to Medusa for persistence.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final MedusaOrderService medusaOrderService;
    private final MedusaCommerceMapper mapper;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        MedusaCheckoutRequest medusaRequest = MedusaCheckoutRequest.builder()
                .cartId(request.getCartId())
                .customerEmail(request.getCustomerEmail())
                .shippingAddressId(request.getShippingAddressId())
                .billingAddressId(request.getBillingAddressId())
                .paymentProviderId(request.getPaymentProviderId())
                .build();
        return mapper.toOrderResponse(medusaOrderService.completeCheckout(medusaRequest));
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        return mapper.toOrderResponse(medusaOrderService.retrieveOrder(orderId));
    }
}
