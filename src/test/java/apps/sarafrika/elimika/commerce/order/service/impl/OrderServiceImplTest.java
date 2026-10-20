package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.internal.config.CommerceCaptureProperties;
import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private InternalOrderService internalOrderService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl service;

    /**
     * Real properties rather than a mock: it is a value holder, and the test reads better stating
     * the environment it is describing than stubbing a getter.
     */
    private void autoCaptureOnComplete(boolean enabled) {
        CommerceCaptureProperties properties = new CommerceCaptureProperties();
        properties.setAutoOnComplete(enabled);
        ReflectionTestUtils.setField(service, "captureProperties", properties);
    }

    private OrderResponse order(String status) {
        return OrderResponse.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .paymentStatus(status)
                .currencyCode("KES")
                .total(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void completeCheckoutAutoCapturesWithoutChargingTheLearnerAPlatformFee() {
        autoCaptureOnComplete(true);
        CheckoutRequest request = new CheckoutRequest();
        when(internalOrderService.completeCheckout(request)).thenReturn(order("AWAITING_PAYMENT"));
        when(internalOrderService.markOrderCaptured("11111111-1111-1111-1111-111111111111"))
                .thenReturn(order("CAPTURED"));

        OrderResponse result = service.completeCheckout(request);

        assertThat(result.getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(result.getPlatformFee()).isNull();

        ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().order().getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(captor.getValue().order().getPlatformFee()).isNull();
    }

    @Test
    void completeCheckoutDoesNotCaptureWhenDisabled() {
        autoCaptureOnComplete(false);
        CheckoutRequest request = new CheckoutRequest();
        when(internalOrderService.completeCheckout(request)).thenReturn(order("AWAITING_PAYMENT"));

        OrderResponse result = service.completeCheckout(request);

        assertThat(result.getPaymentStatus()).isEqualTo("AWAITING_PAYMENT");
        verify(internalOrderService, never()).markOrderCaptured(any());
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }
}
