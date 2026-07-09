package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.commerce.internal.service.impl.PlatformFeeCalculator;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Default {@link OrderService} implementation delegating to the internal commerce stack.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final InternalOrderService internalOrderService;
    private final PlatformFeeCalculator platformFeeCalculator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * When true (dev / no payment gateway), a completed checkout is immediately captured so
     * revenue figures populate. In production this is false and capture is driven by the
     * M-Pesa confirmation callback calling {@link InternalOrderService#markOrderCaptured}.
     */
    @Value("${commerce.capture.auto-on-complete:true}")
    private boolean autoCaptureOnComplete;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        OrderResponse response = internalOrderService.completeCheckout(request);

        if (autoCaptureOnComplete && response != null && response.getId() != null) {
            try {
                OrderResponse captured = internalOrderService.markOrderCaptured(response.getId());
                PlatformFeeBreakdown fee =
                        platformFeeCalculator.compute(captured.getTotal(), captured.getCurrencyCode());
                response = captured.toBuilder().platformFee(fee).build();
            } catch (Exception ex) {
                log.warn("Auto-capture failed for order {}: {}", response.getId(), ex.getMessage());
            }
        }

        eventPublisher.publishEvent(new OrderCompletedEvent(response, request));
        return response;
    }

    @Override
    public OrderResponse getOrder(String orderId) {
        return internalOrderService.getOrder(orderId);
    }
}
