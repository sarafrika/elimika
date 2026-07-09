package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.commerce.internal.service.impl.PlatformFeeCalculator;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
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
    private PlatformFeeCalculator platformFeeCalculator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl service;

    private OrderResponse order(String status) {
        return OrderResponse.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .paymentStatus(status)
                .currencyCode("KES")
                .total(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void completeCheckoutAutoCapturesAndAttachesPlatformFee() {
        ReflectionTestUtils.setField(service, "autoCaptureOnComplete", true);
        CheckoutRequest request = new CheckoutRequest();
        when(internalOrderService.completeCheckout(request)).thenReturn(order("AWAITING_PAYMENT"));
        when(internalOrderService.markOrderCaptured("11111111-1111-1111-1111-111111111111"))
                .thenReturn(order("CAPTURED"));
        PlatformFeeBreakdown fee = new PlatformFeeBreakdown(
                new BigDecimal("25.00"), "KES", null, null, new BigDecimal("1000.00"), null, null);
        when(platformFeeCalculator.compute(new BigDecimal("1000.00"), "KES")).thenReturn(fee);

        OrderResponse result = service.completeCheckout(request);

        assertThat(result.getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(result.getPlatformFee()).isEqualTo(fee);

        ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().order().getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(captor.getValue().order().getPlatformFee()).isEqualTo(fee);
    }

    @Test
    void completeCheckoutDoesNotCaptureWhenDisabled() {
        ReflectionTestUtils.setField(service, "autoCaptureOnComplete", false);
        CheckoutRequest request = new CheckoutRequest();
        when(internalOrderService.completeCheckout(request)).thenReturn(order("AWAITING_PAYMENT"));

        OrderResponse result = service.completeCheckout(request);

        assertThat(result.getPaymentStatus()).isEqualTo("AWAITING_PAYMENT");
        verify(internalOrderService, never()).markOrderCaptured(any());
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }
}
