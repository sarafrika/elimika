package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaOrderService;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.commerce.order.service.PlatformFeeService;
import apps.sarafrika.elimika.commerce.purchase.spi.CommercePurchaseService;
import apps.sarafrika.elimika.commerce.shared.mapper.MedusaCommerceMapper;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
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
    private final CommercePurchaseService commercePurchaseService;
    private final PlatformFeeService platformFeeService;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        MedusaCheckoutRequest medusaRequest = MedusaCheckoutRequest.builder()
                .cartId(request.getCartId())
                .customerEmail(request.getCustomerEmail())
                .shippingAddressId(request.getShippingAddressId())
                .billingAddressId(request.getBillingAddressId())
                .paymentProviderId(request.getPaymentProviderId())
                .build();
        MedusaOrderResponse medusaOrder = medusaOrderService.completeCheckout(medusaRequest);
        OrderResponse response = mapper.toOrderResponse(
                medusaOrder,
                platformFeeService.calculateFee(medusaOrder, request).orElse(null)
        );
        commercePurchaseService.recordOrder(response, request);
        return response;
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        return mapper.toOrderResponse(medusaOrderService.retrieveOrder(orderId));
    }
}
