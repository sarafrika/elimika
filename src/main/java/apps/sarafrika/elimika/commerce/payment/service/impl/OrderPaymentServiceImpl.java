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
}
