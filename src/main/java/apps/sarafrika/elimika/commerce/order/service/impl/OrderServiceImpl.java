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
    private final apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService classEnrolmentGateService;
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
            // No gateway is involved on this path, so nothing has been collected yet and refusing
            // here costs the learner nothing.
            String blocker = findEnrolmentBlocker(response);
            if (blocker != null) {
                log.warn("Auto-capture refused for order {}: {}", response.getId(), blocker);
                throw new IllegalStateException(blocker);
            }
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

    private String findEnrolmentBlocker(OrderResponse order) {
        if (order == null || order.getUserUuid() == null || order.getItems() == null) {
            return null;
        }
        for (var item : order.getItems()) {
            Object classUuid = item.getMetadata() == null ? null : item.getMetadata().get("class_definition_uuid");
            if (classUuid == null) {
                continue;
            }
            try {
                var blocker = classEnrolmentGateService.findEnrolmentBlocker(
                        java.util.UUID.fromString(classUuid.toString()), order.getUserUuid());
                if (blocker.isPresent()) {
                    return blocker.get();
                }
            } catch (IllegalArgumentException ignored) {
                // Not a compliance failure; leave it to the enrolment path.
            }
        }
        return null;
    }
}
