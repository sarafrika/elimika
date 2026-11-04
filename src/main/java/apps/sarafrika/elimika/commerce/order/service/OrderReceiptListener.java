package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the receipt once an order is captured, off the request thread.
 * <p>
 * Capture is published both inside a transaction (the sweep) and outside one (the browser poll), so
 * this runs either way, and in its own transaction: the email is handed to notifications as an
 * after-commit event, which is silently never delivered when published with no transaction open.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReceiptListener {

    private final OrderReceiptNotifier orderReceiptNotifier;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        try {
            orderReceiptNotifier.sendReceipt(event.order());
        } catch (Exception ex) {
            // A receipt must never undo or retry a capture; the payment is already recorded.
            log.error("Receipt for order {} could not be sent: {}", event.order().getDisplayId(), ex.getMessage(), ex);
        }
    }
}
