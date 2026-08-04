package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.spi.CommercePurchaseService;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Records the purchase behind a completed order.
 * <p>
 * This is deliberately the <em>first</em> reaction to {@code OrderCompletedEvent}. Payout credits
 * earners off the request thread and then stamps back onto these rows what it did with each line's
 * money; running synchronously and first means the rows it stamps already exist, rather than the
 * write-back racing the recording it depends on.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionListener {

    private final CommercePurchaseService commercePurchaseService;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        log.debug("Recording purchase for order {}", event.order().getId());
        commercePurchaseService.recordOrder(event.order(), event.checkoutRequest());
    }
}
