package apps.sarafrika.elimika.commerce.payment.service.impl;

import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.commerce.internal.service.impl.PlatformFeeCalculator;
import apps.sarafrika.elimika.commerce.payment.client.MpesaGatewayClient;
import apps.sarafrika.elimika.commerce.payment.dto.MpesaCheckoutResponse;
import apps.sarafrika.elimika.commerce.payment.dto.PaymentStatusResponse;
import apps.sarafrika.elimika.commerce.payment.service.OrderPaymentService;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default {@link OrderPaymentService} wiring the mpesa-service gateway to the internal capture seam.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private static final String STATUS_AWAITING_PAYMENT = "AWAITING_PAYMENT";
    private static final String STATUS_CAPTURED = "CAPTURED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String GATEWAY_STATUS_SUCCESS = "SUCCESS";

    private final InternalOrderService internalOrderService;
    private final MpesaGatewayClient mpesaGatewayClient;
    private final PlatformFeeCalculator platformFeeCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassEnrolmentGateService classEnrolmentGateService;

    @Override
    public MpesaCheckoutResponse initiateMpesaPayment(String orderId, String phoneNumber) {
        OrderResponse order = internalOrderService.getOrder(orderId);
        if (!STATUS_AWAITING_PAYMENT.equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException(
                    "Order " + orderId + " is not awaiting payment (status=" + order.getPaymentStatus() + ")");
        }

        String checkoutRequestId = mpesaGatewayClient.initiateStkPush(
                phoneNumber,
                order.getTotal(),
                orderId,
                "Payment for order " + orderId);
        internalOrderService.storeCheckoutRequestId(orderId, checkoutRequestId);

        return new MpesaCheckoutResponse(checkoutRequestId, STATUS_PENDING);
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(String orderId) {
        OrderResponse order = internalOrderService.getOrder(orderId);
        if (STATUS_CAPTURED.equalsIgnoreCase(order.getPaymentStatus())) {
            return new PaymentStatusResponse(STATUS_CAPTURED);
        }

        String checkoutRequestId = internalOrderService.findCheckoutRequestId(orderId)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalStateException(
                        "No M-Pesa checkout initiated for order " + orderId));

        String gatewayStatus = mpesaGatewayClient.getPaymentStatus(checkoutRequestId);
        if (GATEWAY_STATUS_SUCCESS.equalsIgnoreCase(gatewayStatus)) {
            String blocker = findEnrolmentBlocker(order);
            if (blocker != null) {
                // The rules changed while the learner was paying. The capture is refused, so no
                // record of the money is written on this side and the seat is not granted. Safaricom
                // has already collected it, so this line is the only trace there will be — it must
                // be loud enough for somebody to act on.
                log.error("REFUSED CAPTURE for order {}: {}. M-Pesa checkout {} has already collected"
                        + " {} {} and no capture was recorded. Manual reversal required.",
                        orderId, blocker, checkoutRequestId, order.getTotal(), order.getCurrencyCode());
                return new PaymentStatusResponse("REFUSED");
            }
            OrderResponse captured = internalOrderService.markOrderCaptured(orderId);
            PlatformFeeBreakdown fee =
                    platformFeeCalculator.compute(captured.getTotal(), captured.getCurrencyCode());
            OrderResponse enriched = captured.toBuilder().platformFee(fee).build();
            eventPublisher.publishEvent(new OrderCompletedEvent(enriched, null));
            log.info("Captured order {} on confirmed M-Pesa payment {}", orderId, checkoutRequestId);
            return new PaymentStatusResponse(STATUS_CAPTURED);
        }

        return new PaymentStatusResponse(gatewayStatus);
    }

    /**
     * Re-asks the compliance gate for every class on this order, at the moment of recording payment.
     */
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
                // A malformed identifier is not a compliance failure; leave it to the enrolment path.
            }
        }
        return null;
    }
}
