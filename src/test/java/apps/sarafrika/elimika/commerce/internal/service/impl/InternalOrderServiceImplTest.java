package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.internal.config.PaymentCallbackProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalOrderServiceImplTest {

    private static final String CART_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ORDER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID BUYER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_BUYER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final String CHECKOUT_REQUEST_ID = "ws_CO_1";

    @Mock
    private InternalCartService internalCartService;
    @Mock
    private CommercePaymentRepository paymentRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private InternalCommerceMapper mapper;
    @Mock
    private DomainSecurityService domainSecurityService;

    private InternalOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalOrderServiceImpl(
                internalCartService,
                paymentRepository,
                orderRepository,
                mapper,
                domainSecurityService,
                new PaymentCallbackProperties());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeCheckoutReturnsExistingOrderForRepeatedCartCheckout() {
        CommerceOrder existingOrder = orderOwnedBy(BUYER);
        OrderResponse response = order("AWAITING_PAYMENT");
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
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

    @Test
    void completeCheckoutIgnoresAnOrderPlacedOnThatCartByAnotherBuyer() {
        OrderResponse response = order("AWAITING_PAYMENT");
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(orderOwnedBy(OTHER_BUYER)));
        when(internalCartService.completeCart(CART_ID)).thenReturn(response);

        assertThat(service.completeCheckout(checkoutRequest())).isSameAs(response);
        verify(mapper, never()).toOrderResponse(any());
    }

    @Test
    void getOrderHidesAnotherBuyersOrder() {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(orderRepository.findByUuid(UUID.fromString(ORDER_ID)))
                .thenReturn(Optional.of(orderOwnedBy(OTHER_BUYER)));

        assertThatThrownBy(() -> service.getOrder(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    void getOrderServesTheGatewaySettlingAPaymentItStarted() {
        CommerceOrder awaitingSettlement = orderOwnedBy(OTHER_BUYER);
        awaitingSettlement.setCheckoutRequestId(CHECKOUT_REQUEST_ID);
        OrderResponse response = order("AWAITING_PAYMENT");
        authenticateAsServiceAccountOf("mpesa-service");
        when(orderRepository.findByUuid(UUID.fromString(ORDER_ID)))
                .thenReturn(Optional.of(awaitingSettlement));
        when(mapper.toOrderResponse(awaitingSettlement)).thenReturn(response);

        assertThat(service.getOrder(ORDER_ID)).isSameAs(response);
    }

    @Test
    void getOrderRefusesAServiceAccountThatIsNotTheGateway() {
        CommerceOrder awaitingSettlement = orderOwnedBy(OTHER_BUYER);
        awaitingSettlement.setCheckoutRequestId(CHECKOUT_REQUEST_ID);
        authenticateAsServiceAccountOf("some-other-integration");
        when(orderRepository.findByUuid(UUID.fromString(ORDER_ID)))
                .thenReturn(Optional.of(awaitingSettlement));

        assertThatThrownBy(() -> service.getOrder(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderRefusesTheGatewayOnAnOrderWithNoPaymentInFlight() {
        authenticateAsServiceAccountOf("mpesa-service");
        when(orderRepository.findByUuid(UUID.fromString(ORDER_ID)))
                .thenReturn(Optional.of(orderOwnedBy(OTHER_BUYER)));

        assertThatThrownBy(() -> service.getOrder(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void storeCheckoutRequestIdIsClosedToTheGateway() {
        CommerceOrder awaitingSettlement = orderOwnedBy(OTHER_BUYER);
        awaitingSettlement.setCheckoutRequestId(CHECKOUT_REQUEST_ID);
        authenticateAsServiceAccountOf("mpesa-service");
        when(orderRepository.findByUuid(UUID.fromString(ORDER_ID)))
                .thenReturn(Optional.of(awaitingSettlement));

        assertThatThrownBy(() -> service.storeCheckoutRequestId(ORDER_ID, "ws_CO_2"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    private void authenticateAsServiceAccountOf(String clientId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "service-account-" + clientId)
                .claim("azp", clientId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private CommerceOrder orderOwnedBy(UUID buyerUuid) {
        CommerceOrder order = new CommerceOrder();
        order.setUserUuid(buyerUuid);
        return order;
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
