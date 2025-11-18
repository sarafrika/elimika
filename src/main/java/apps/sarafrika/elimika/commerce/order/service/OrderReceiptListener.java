package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReceiptListener {

    private final OrderReceiptNotifier orderReceiptNotifier;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null || event.checkoutRequest() == null) {
            return;
        }
        log.debug("Sending order receipt for order {}", event.order().getId());
        orderReceiptNotifier.sendReceipt(event.order(), event.checkoutRequest());
    }
}
