package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCart;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCartItem;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.RegionResolver;
import apps.sarafrika.elimika.shared.currency.service.CurrencyValidator;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassCapacityService;
import apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCartServiceImplTest {

    private static final String CART_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ORDER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String VARIANT_ID = "class-seat";
    private static final UUID BUYER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_BUYER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CommerceCartRepository cartRepository;
    @Mock
    private CommerceCartItemRepository cartItemRepository;
    @Mock
    private CommerceProductVariantRepository variantRepository;
    @Mock
    private CommerceCatalogueItemRepository catalogItemRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private CommerceOrderItemRepository orderItemRepository;
    @Mock
    private InternalCommerceMapper mapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RegionResolver regionResolver;
    @Mock
    private ClassCapacityService classCapacityService;
    @Mock
    private ClassEnrolmentGateService classEnrolmentGateService;
    @Mock
    private ClassScheduleService classScheduleService;
    @Mock
    private CurrencyValidator currencyValidator;
    @Mock
    private DomainSecurityService domainSecurityService;

    private InternalCartServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalCartServiceImpl(
                cartRepository,
                cartItemRepository,
                variantRepository,
                catalogItemRepository,
                orderRepository,
                orderItemRepository,
                mapper,
                objectMapper,
                regionResolver,
                classCapacityService,
                classEnrolmentGateService,
                classScheduleService,
                currencyValidator,
                domainSecurityService);
    }

    @Test
    void completeCartReturnsExistingOrderWhenCartIsAlreadyCompleted() {
        CommerceCart cart = completedCartWithItem();
        CommerceOrder existingOrder = orderOwnedBy(BUYER);
        OrderResponse response = order("AWAITING_PAYMENT");
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(existingOrder));
        when(mapper.toOrderResponse(existingOrder)).thenReturn(response);

        OrderResponse result = service.completeCart(CART_ID);

        assertThat(result).isSameAs(response);
        verify(orderItemRepository, never()).saveAll(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemReturnsCompletedCartWhenLineAlreadyExists() {
        CommerceCart cart = completedCartWithItem();
        CommerceOrder existingOrder = orderOwnedBy(BUYER);
        CartResponse response = cartResponse("COMPLETED");
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(existingOrder));
        when(mapper.toCartResponse(cart)).thenReturn(response);

        CartResponse result = service.addItem(CART_ID, lineItemRequest(VARIANT_ID, 1));

        assertThat(result).isSameAs(response);
        verify(variantRepository, never()).findByCode(any());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemStillRefusesCompletedCartWhenLineDoesNotMatch() {
        CommerceCart cart = completedCartWithItem();
        CommerceOrder existingOrder = orderOwnedBy(BUYER);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> service.addItem(CART_ID, lineItemRequest("different-seat", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cart is not open");
    }

    @Test
    void getCartHidesAnotherBuyersCart() {
        CommerceCart cart = completedCartWithItem();
        cart.setUserUuid(OTHER_BUYER);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.getCart(CART_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found");
    }

    @Test
    void getCartHidesACartNobodyIsRecordedAgainst() {
        CommerceCart cart = completedCartWithItem();
        cart.setUserUuid(null);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.getCart(CART_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void completeCartDoesNotHandBackAnOrderPlacedByAnotherBuyer() {
        CommerceCart cart = completedCartWithItem();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(BUYER);
        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));
        when(orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(UUID.fromString(CART_ID)))
                .thenReturn(Optional.of(orderOwnedBy(OTHER_BUYER)));

        assertThatThrownBy(() -> service.completeCart(CART_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cart is not open");
        verify(mapper, never()).toOrderResponse(any());
    }

    private CommerceCart completedCartWithItem() {
        CommerceCart cart = new CommerceCart();
        cart.setUuid(UUID.fromString(CART_ID));
        cart.setUserUuid(BUYER);
        cart.setStatus(CartStatus.COMPLETED);
        cart.setItems(List.of(cartItem()));
        return cart;
    }

    private CommerceOrder orderOwnedBy(UUID buyerUuid) {
        CommerceOrder order = new CommerceOrder();
        order.setUserUuid(buyerUuid);
        return order;
    }

    private CommerceCartItem cartItem() {
        CommerceProductVariant variant = new CommerceProductVariant();
        variant.setCode(VARIANT_ID);

        CommerceCartItem item = new CommerceCartItem();
        item.setVariant(variant);
        item.setQuantity(1);
        return item;
    }

    private CartLineItemRequest lineItemRequest(String variantId, int quantity) {
        CartLineItemRequest request = new CartLineItemRequest();
        request.setVariantId(variantId);
        request.setQuantity(quantity);
        return request;
    }

    private CartResponse cartResponse(String status) {
        return new CartResponse(
                CART_ID,
                "KES",
                "KE",
                status,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                List.of());
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
