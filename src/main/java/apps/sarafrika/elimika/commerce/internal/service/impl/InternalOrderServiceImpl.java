package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalOrderServiceImpl implements InternalOrderService {

    private final InternalCartService internalCartService;
    private final CommerceOrderRepository orderRepository;
    private final InternalCommerceMapper mapper;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        UpdateCartRequest updateCartRequest = UpdateCartRequest.builder()
                .email(request.getCustomerEmail())
                .shippingAddressId(request.getShippingAddressId())
                .billingAddressId(request.getBillingAddressId())
                .build();
        internalCartService.updateCart(request.getCartId(), updateCartRequest);
        if (StringUtils.hasText(request.getPaymentProviderId())) {
            SelectPaymentSessionRequest paymentRequest = SelectPaymentSessionRequest.builder()
                    .providerId(request.getPaymentProviderId())
                    .build();
            internalCartService.selectPaymentSession(request.getCartId(), paymentRequest);
        }
        return internalCartService.completeCart(request.getCartId());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        UUID uuid = parseUuid(orderId);
        CommerceOrder order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return mapper.toOrderResponse(order);
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order identifier", ex);
        }
    }
}
