package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalOrderServiceImplTest {

    private static final String CART_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ORDER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Mock
    private InternalCartService internalCartService;
    @Mock
    private CommercePaymentRepository paymentRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private InternalCommerceMapper mapper;

    private InternalOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalOrderServiceImpl(
                internalCartService,
                paymentRepository,
                orderRepository,
                mapper);
    }

    @Test
    void completeCheckoutReturnsExistingOrderForRepeatedCartCheckout() {
        CommerceOrder existingOrder = new CommerceOrder();
        OrderResponse response = order("AWAITING_PAYMENT");
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(existingOrder));
        when(mapper.toOrderResponse(existingOrder)).thenReturn(response);

        OrderResponse result = service.completeCheckout(checkoutRequest());

        assertThat(result).isSameAs(response);
        verify(internalCartService, never()).updateCart(any(), any());
        verify(internalCartService, never()).selectPaymentSession(any(), any());
        verify(internalCartService, never()).completeCart(any());
    }

    @Test
    void completeCheckoutCreatesOrderWhenCartHasNotBeenCompleted() {
        OrderResponse response = order("AWAITING_PAYMENT");
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.empty());
        when(internalCartService.completeCart(CART_ID)).thenReturn(response);

        OrderResponse result = service.completeCheckout(checkoutRequest());

        assertThat(result).isSameAs(response);

        ArgumentCaptor<UpdateCartRequest> updateCaptor = ArgumentCaptor.forClass(UpdateCartRequest.class);
        verify(internalCartService).updateCart(org.mockito.ArgumentMatchers.eq(CART_ID), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getEmail()).isEqualTo("learner@example.com");

        ArgumentCaptor<SelectPaymentSessionRequest> paymentCaptor =
                ArgumentCaptor.forClass(SelectPaymentSessionRequest.class);
        verify(internalCartService).selectPaymentSession(org.mockito.ArgumentMatchers.eq(CART_ID), paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getProviderId()).isEqualTo("mpesa");
    }

    private CheckoutRequest checkoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setCartId(CART_ID);
        request.setCustomerEmail("learner@example.com");
        request.setPaymentProviderId("mpesa");
        return request;
    }

    private OrderResponse order(String paymentStatus) {
        return new OrderResponse(
                ORDER_ID,
                "1001",
                null,
                paymentStatus,
                "KES",
                new BigDecimal("1000.0000"),
                new BigDecimal("1000.0000"),
                null,
                null,
                List.of());
    }
}
