package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Default {@link OrderService} implementation delegating to the internal commerce stack.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final InternalOrderService internalOrderService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        OrderResponse response = internalOrderService.completeCheckout(request);
        eventPublisher.publishEvent(new OrderCompletedEvent(response, request));
        return response;
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        return internalOrderService.getOrder(orderId);
    }
}
