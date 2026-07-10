package apps.sarafrika.elimika.commerce.payment.service.impl;

import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.commerce.internal.service.impl.PlatformFeeCalculator;
import apps.sarafrika.elimika.commerce.payment.client.MpesaGatewayClient;
import apps.sarafrika.elimika.commerce.payment.dto.MpesaCheckoutResponse;
import apps.sarafrika.elimika.commerce.payment.dto.PaymentStatusResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceImplTest {

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CHECKOUT_ID = "ws_CO_04112017184930742";

    @Mock
    private InternalOrderService internalOrderService;
    @Mock
    private MpesaGatewayClient mpesaGatewayClient;
    @Mock
    private PlatformFeeCalculator platformFeeCalculator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderPaymentServiceImpl service;

    private OrderResponse order(String status) {
        return OrderResponse.builder()
                .id(ORDER_ID)
                .paymentStatus(status)
                .currencyCode("KES")
                .total(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void initiateMpesaPaymentInitiatesAndStoresCheckoutRequestId() {
        when(internalOrderService.getOrder(ORDER_ID)).thenReturn(order("AWAITING_PAYMENT"));
        when(mpesaGatewayClient.initiateStkPush(eq("254708374149"), eq(new BigDecimal("1000.00")),
                eq(ORDER_ID), any())).thenReturn(CHECKOUT_ID);

        MpesaCheckoutResponse response = service.initiateMpesaPayment(ORDER_ID, "254708374149");

        assertThat(response.checkoutRequestId()).isEqualTo(CHECKOUT_ID);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(internalOrderService).storeCheckoutRequestId(ORDER_ID, CHECKOUT_ID);
    }

    @Test
    void initiateMpesaPaymentRejectsOrderNotAwaitingPayment() {
        when(internalOrderService.getOrder(ORDER_ID)).thenReturn(order("CAPTURED"));

        assertThatThrownBy(() -> service.initiateMpesaPayment(ORDER_ID, "254708374149"))
                .isInstanceOf(IllegalStateException.class);

        verify(mpesaGatewayClient, never()).initiateStkPush(any(), any(), any(), any());
        verify(internalOrderService, never()).storeCheckoutRequestId(any(), any());
    }

    @Test
    void getPaymentStatusCapturesOnceOnSuccess() {
        when(internalOrderService.getOrder(ORDER_ID)).thenReturn(order("AWAITING_PAYMENT"));
        when(internalOrderService.findCheckoutRequestId(ORDER_ID)).thenReturn(Optional.of(CHECKOUT_ID));
        when(mpesaGatewayClient.getPaymentStatus(CHECKOUT_ID)).thenReturn("SUCCESS");
        when(internalOrderService.markOrderCaptured(ORDER_ID)).thenReturn(order("CAPTURED"));
        PlatformFeeBreakdown fee = new PlatformFeeBreakdown(
                new BigDecimal("25.00"), "KES", null, null, new BigDecimal("1000.00"), null, null);
        when(platformFeeCalculator.compute(new BigDecimal("1000.00"), "KES")).thenReturn(fee);

        PaymentStatusResponse response = service.getPaymentStatus(ORDER_ID);

        assertThat(response.status()).isEqualTo("CAPTURED");
        verify(internalOrderService).markOrderCaptured(ORDER_ID);

        ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().order().getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(captor.getValue().order().getPlatformFee()).isEqualTo(fee);
        assertThat(captor.getValue().checkoutRequest()).isNull();
    }

    @Test
    void getPaymentStatusIsIdempotentWhenAlreadyCaptured() {
        when(internalOrderService.getOrder(ORDER_ID)).thenReturn(order("CAPTURED"));

        PaymentStatusResponse response = service.getPaymentStatus(ORDER_ID);

        assertThat(response.status()).isEqualTo("CAPTURED");
        verify(internalOrderService, never()).markOrderCaptured(any());
        verify(mpesaGatewayClient, never()).getPaymentStatus(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getPaymentStatusDoesNotCaptureWhenNotSuccessful() {
        when(internalOrderService.getOrder(ORDER_ID)).thenReturn(order("AWAITING_PAYMENT"));
        when(internalOrderService.findCheckoutRequestId(ORDER_ID)).thenReturn(Optional.of(CHECKOUT_ID));
        when(mpesaGatewayClient.getPaymentStatus(CHECKOUT_ID)).thenReturn("PENDING");

        PaymentStatusResponse response = service.getPaymentStatus(ORDER_ID);

        assertThat(response.status()).isEqualTo("PENDING");
        verify(internalOrderService, never()).markOrderCaptured(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
