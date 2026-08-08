package apps.sarafrika.elimika.commerce.payment.internal;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.payment.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Settles M-Pesa payments whose outcome never reached us through the browser.
 *
 * <h2>Why this exists</h2>
 * Capture used to happen only while the student's page was polling. A learner who approved the
 * prompt and then closed the tab had their money taken by Safaricom and got no seat, with no code
 * anywhere that would notice.
 *
 * <h2>Cadence</h2>
 * An STK prompt lives about a minute, so the timings are deliberately short: anything still
 * unresolved after 90 seconds is re-queried every minute, and anything past five minutes is given up
 * on and its held seat released. A slower sweep would be useless to the person watching their phone
 * and would leave seats dead long after the buyer walked away.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MpesaPaymentReconciliationScheduler {

    private static final int UNRESOLVED_AFTER_SECONDS = 90;
    private static final int GIVE_UP_AFTER_MINUTES = 5;

    private final CommerceOrderRepository orderRepository;
    private final OrderPaymentService orderPaymentService;

    @Scheduled(
            initialDelayString = "${commerce.payment.reconciliation.initial-delay:PT60S}",
            fixedDelayString = "${commerce.payment.reconciliation.interval:PT60S}")
    @Transactional
    void settleUnresolvedPayments() {
        try {
            reconcile();
        } catch (Exception ex) {
            // Never let a sweep kill the scheduler. Unresolved payments stay unresolved and the next
            // pass picks them up; a database that has gone away is not worth a stack trace a minute.
            log.warn("M-Pesa reconciliation sweep could not run: {}", ex.getMessage());
        }
    }

    private void reconcile() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<CommerceOrder> unresolved = orderRepository
                .findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtBefore(
                        PaymentStatus.AWAITING_PAYMENT, now.minusSeconds(UNRESOLVED_AFTER_SECONDS));

        if (unresolved.isEmpty()) {
            return;
        }

        for (CommerceOrder order : unresolved) {
            String orderId = order.getUuid() == null ? null : order.getUuid().toString();
            if (orderId == null) {
                continue;
            }
            try {
                // Asking for the status is what captures it, so this is both the question and the
                // settlement. Enrolment follows from the same event the browser path publishes.
                orderPaymentService.getPaymentStatus(orderId);
            } catch (Exception e) {
                log.warn("Could not settle payment for order {}: {}", orderId, e.getMessage());
            }

            if (order.getPlacedAt() != null
                    && order.getPlacedAt().isBefore(now.minusMinutes(GIVE_UP_AFTER_MINUTES))
                    && order.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT) {
                order.setPaymentStatus(PaymentStatus.CANCELED);
                orderRepository.save(order);
                log.info("Abandoned order {} past the STK window; its held seats will lapse", orderId);
            }
        }
    }
}
