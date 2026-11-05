package apps.sarafrika.elimika.commerce.payment.internal;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrderItem;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.service.LearnerResolver;
import apps.sarafrika.elimika.commerce.payment.service.OrderPaymentService;
import apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Settles M-Pesa payments whose outcome never reached us through the browser.
 *
 * <h2>Why this exists</h2>
 * Capture used to happen only while the student's page was polling. A learner who approved the
 * prompt and then closed the tab had their money taken by Safaricom and got no seat, with no code
 * anywhere that would notice.
 *
 * <h2>Cadence</h2>
 * Unpaid orders are re-queried every five seconds, so a payment is confirmed within seconds even
 * with the tab closed. Windows run from the latest prompt: five minutes after M-Pesa answers that it
 * failed, or fifteen while it still says pending (its own Safaricom query needs that long when a
 * callback is lost). Only a failed answer may tell the buyer nothing was charged. For an hour after
 * cancelling it is still re-checked once a minute, in case M-Pesa confirms late.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MpesaPaymentReconciliationScheduler {

    private static final int UNRESOLVED_AFTER_SECONDS = 5;
    private static final int GIVE_UP_AFTER_MINUTES = 5;
    private static final int GIVE_UP_WHILE_PENDING_MINUTES = 15;
    private static final java.util.Set<String> FAILED_ANSWERS = java.util.Set.of("FAILED", "CANCELLED", "CANCELED");
    private static final int LATE_CONFIRMATION_MINUTES = 60;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CommerceOrderRepository orderRepository;
    private final OrderPaymentService orderPaymentService;
    private final ClassEnrolmentGateService classEnrolmentGateService;
    private final LearnerResolver learnerResolver;
    private final ObjectMapper objectMapper;
    private final apps.sarafrika.elimika.commerce.order.service.OrderReceiptNotifier receiptNotifier;
    private final apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository paymentRepository;
    private final apps.sarafrika.elimika.commerce.payment.client.MpesaGatewayClient mpesaGatewayClient;

    @Scheduled(
            initialDelayString = "${commerce.payment.reconciliation.initial-delay:PT60S}",
            fixedDelayString = "${commerce.payment.reconciliation.interval:PT60S}")
    @Transactional
    void settleUnresolvedPayments() {
        try {
            reconcile();
        } catch (Exception ex) {
            // Never let a sweep kill the scheduler. Unresolved payments stay unresolved and the next
            // pass picks them up.
            log.warn("M-Pesa reconciliation sweep could not run: {}", ex.getMessage());
        }
    }

    @Scheduled(initialDelayString = "PT60S", fixedDelayString = "PT60S")
    @Transactional
    void recheckRecentlyCancelled() {
        try {
            // M-Pesa can confirm after we gave up (a slow PIN, a late callback); keep asking for an hour.
            LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(LATE_CONFIRMATION_MINUTES);
            for (CommerceOrder order : orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtAfter(
                    PaymentStatus.CANCELED, since)) {
                try {
                    orderPaymentService.getPaymentStatus(order.getUuid().toString());
                } catch (Exception e) {
                    log.warn("Could not re-check cancelled order {}: {}", order.getOrderNumber(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Re-check of cancelled M-Pesa orders could not run: {}", ex.getMessage());
        }
    }

    private void reconcile() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime giveUpBefore = now.minusMinutes(GIVE_UP_AFTER_MINUTES);

        for (CommerceOrder order : orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtBefore(
                PaymentStatus.AWAITING_PAYMENT, now.minusSeconds(UNRESOLVED_AFTER_SECONDS))) {
            String orderId = order.getUuid() == null ? null : order.getUuid().toString();
            if (orderId == null) {
                continue;
            }
            String answer = null;
            try {
                // Asking for the status is what captures it, so this is both the question and the
                // settlement. Enrolment follows from the same event the browser path publishes.
                var status = orderPaymentService.getPaymentStatus(orderId);
                answer = status == null || status.status() == null ? null : status.status().toUpperCase(java.util.Locale.ROOT);
            } catch (Exception e) {
                log.warn("Could not settle payment for order {}: {}", orderId, e.getMessage());
            }
            decideHold(order, answer, now);
        }

        // A prompt that never started can never be paid; cancel it once the same window has passed.
        orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNullAndPlacedAtBefore(
                PaymentStatus.AWAITING_PAYMENT, giveUpBefore).forEach(this::abandon);
    }

    /**
     * Cancels only on a final failed answer after five minutes, or after fifteen with no answer; a
     * paid or refused order is never touched here. Times run from the latest prompt.
     */
    void decideHold(CommerceOrder order, String answer, LocalDateTime now) {
        LocalDateTime since = order.getPromptSentAt() != null ? order.getPromptSentAt() : order.getPlacedAt();
        if (since == null || "CAPTURED".equals(answer) || "SUCCESS".equals(answer) || "REFUSED".equals(answer)) {
            return;
        }
        boolean failed = answer != null && FAILED_ANSWERS.contains(answer);
        if (failed && since.isBefore(now.minusMinutes(GIVE_UP_AFTER_MINUTES))) {
            abandon(order, true);
        } else if (!failed && since.isBefore(now.minusMinutes(GIVE_UP_WHILE_PENDING_MINUTES))) {
            log.warn("Order {} still has no final M-Pesa answer {} minutes after its prompt; releasing the seat"
                    + " and re-checking for an hour", order.getOrderNumber(), GIVE_UP_WHILE_PENDING_MINUTES);
            abandon(order, false);
        }
    }

    /**
     * Fills the receipt of payments captured before M-Pesa had one (its query path carries none; the
     * receipt comes with a later C2B confirmation). Receipts already sent simply lacked the code.
     */
    @Scheduled(initialDelayString = "PT90S", fixedDelayString = "PT60S")
    @Transactional
    void backfillMissingReceipts() {
        try {
            LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(48);
            for (var payment : paymentRepository.findByStatusAndProviderReceiptIsNullAndExternalReferenceIsNotNullAndProcessedAtAfter(
                    PaymentStatus.CAPTURED, since)) {
                try {
                    String receipt = apps.sarafrika.elimika.commerce.internal.service.impl.MpesaReceipts.orNull(
                            mpesaGatewayClient.getPayment(payment.getExternalReference()).transactionId());
                    if (receipt != null) {
                        payment.setProviderReceipt(receipt);
                        paymentRepository.save(payment);
                        log.info("Attached late M-Pesa receipt {} to payment {}", receipt, payment.getExternalReference());
                    }
                } catch (Exception e) {
                    log.debug("Receipt not yet available for {}: {}", payment.getExternalReference(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Receipt backfill could not run: {}", ex.getMessage());
        }
    }

    private void abandon(CommerceOrder order) {
        abandon(order, true);
    }

    /** {@code confirmedUnpaid}: M-Pesa said the prompt failed, so the buyer may be told nothing was charged. */
    private void abandon(CommerceOrder order, boolean confirmedUnpaid) {
        if (order.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT) {
            return;
        }
        order.setPaymentStatus(PaymentStatus.CANCELED);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        releaseHeldSeats(order);
        log.info("Abandoned unpaid order {} past the STK window and released its held seats", order.getOrderNumber());
        // The seat-hold window ran out: one of the only two moments a buyer is messaged.
        if (confirmedUnpaid && order.getPromptMsisdn() != null) {
            try {
                receiptNotifier.sendSeatReleased(order.getOrderNumber(), firstTitle(order), order.getPromptMsisdn());
            } catch (Exception ex) {
                log.warn("Seat-released SMS for order {} could not be sent: {}", order.getOrderNumber(), ex.getMessage());
            }
        }
    }

    /** Gives back every seat this order held; seats already taken by a paid enrolment are untouched. */
    private void releaseHeldSeats(CommerceOrder order) {
        if (order.getItems() == null || order.getUserUuid() == null) {
            return;
        }
        for (CommerceOrderItem item : order.getItems()) {
            try {
                Map<String, Object> metadata = readMetadata(item.getMetadataJson());
                Object classUuid = metadata.get("class_definition_uuid");
                if (classUuid == null) {
                    continue;
                }
                classEnrolmentGateService.releaseSeats(UUID.fromString(classUuid.toString()),
                        learnerResolver.learnerUserUuid(metadata, order.getUserUuid()));
            } catch (Exception ex) {
                log.warn("Could not release held seat for order {} line {}: {}",
                        order.getOrderNumber(), item.getUuid(), ex.getMessage());
            }
        }
    }

    private static String firstTitle(CommerceOrder order) {
        return order.getItems() == null || order.getItems().isEmpty() ? null : order.getItems().getFirst().getTitle();
    }

    private Map<String, Object> readMetadata(String json) throws Exception {
        return StringUtils.hasText(json) ? objectMapper.readValue(json, MAP_TYPE) : Map.of();
    }
}
