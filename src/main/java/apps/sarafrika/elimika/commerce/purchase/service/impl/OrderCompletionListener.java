package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.spi.CommercePurchaseService;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionListener {

    private final CommercePurchaseService commercePurchaseService;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        log.debug("Recording purchase for order {}", event.order().getId());
        commercePurchaseService.recordOrder(event.order(), event.checkoutRequest());
    }
}
