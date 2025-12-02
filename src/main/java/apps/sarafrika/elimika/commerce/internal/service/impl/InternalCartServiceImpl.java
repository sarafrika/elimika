package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CreateCartRequest;
import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCart;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCartItem;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrderItem;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.commerce.internal.service.RegionResolver;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalCartServiceImpl implements InternalCartService {

    private static final BigDecimal ZERO = BigDecimal.valueOf(0, 4);

    private final CommerceCartRepository cartRepository;
    private final CommerceCartItemRepository cartItemRepository;
    private final CommerceProductVariantRepository variantRepository;
    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final InternalCommerceMapper mapper;
    private final ObjectMapper objectMapper;
    private final RegionResolver regionResolver;

    @Override
    public CartResponse createCart(CreateCartRequest request) {
        String currencyCode = request.getCurrencyCode();
        if (!StringUtils.hasText(currencyCode)) {
            throw new IllegalArgumentException("currency_code is required when internal commerce is enabled");
        }

        CommerceCart cart = new CommerceCart();
        cart.setStatus(CartStatus.OPEN);
        cart.setCurrencyCode(currencyCode);
        cart.setRegionCode(regionResolver.resolveRegionCode(request.getRegionCode(), null));
        cart.setMetadataJson(writeMetadata(sanitizeMetadata(request.getMetadata())));
        cart = cartRepository.save(cart);

        if (!CollectionUtils.isEmpty(request.getItems())) {
            for (CartLineItemRequest item : request.getItems()) {
                addItemToCart(cart, item);
            }
        }

        recalcTotals(cart);
        cartRepository.save(cart);
        return mapper.toCartResponse(cart);
    }

    @Override
    public CartResponse addItem(String cartId, CartLineItemRequest request) {
        CommerceCart cart = loadCart(cartId);
        ensureOpen(cart);
        addItemToCart(cart, request);
        recalcTotals(cart);
        cartRepository.save(cart);
        return mapper.toCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String cartId) {
        return mapper.toCartResponse(loadCart(cartId));
    }

    @Override
    public CartResponse updateCart(String cartId, UpdateCartRequest request) {
        CommerceCart cart = loadCart(cartId);
        ensureOpen(cart);

        Map<String, Object> metadata = mergeMetadata(cart.getMetadataJson(), request.getMetadata());
        if (StringUtils.hasText(request.getEmail())) {
            metadata.put("customer_email", request.getEmail());
        }
        if (StringUtils.hasText(request.getCustomerId())) {
            metadata.put("customer_id", request.getCustomerId());
        }
        if (StringUtils.hasText(request.getShippingAddressId())) {
            metadata.put("shipping_address_id", request.getShippingAddressId());
        }
        if (StringUtils.hasText(request.getBillingAddressId())) {
            metadata.put("billing_address_id", request.getBillingAddressId());
        }

        cart.setMetadataJson(writeMetadata(metadata));
        cartRepository.save(cart);
        return mapper.toCartResponse(cart);
    }

    @Override
    public CartResponse selectPaymentSession(String cartId, SelectPaymentSessionRequest request) {
        CommerceCart cart = loadCart(cartId);
        ensureOpen(cart);
        Map<String, Object> metadata = mergeMetadata(cart.getMetadataJson(), Map.of());
        metadata.put("payment_provider_id", request.getProviderId());
        cart.setMetadataJson(writeMetadata(metadata));
        cartRepository.save(cart);
        return mapper.toCartResponse(cart);
    }

    @Override
    public OrderResponse completeCart(String cartId) {
        CommerceCart cart = loadCart(cartId);
        ensureOpen(cart);
        if (CollectionUtils.isEmpty(cart.getItems())) {
            throw new IllegalStateException("Cart has no items");
        }

        String customerEmail = resolveCustomerEmail(cart);
        if (!StringUtils.hasText(customerEmail)) {
            throw new IllegalStateException("Customer email is required to complete checkout");
        }

        CommerceOrder order = new CommerceOrder();
        order.setCart(cart);
        order.setUserUuid(cart.getUserUuid());
        order.setCustomerEmail(customerEmail);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
        order.setCurrencyCode(cart.getCurrencyCode());
        order.setSubtotalAmount(cart.getSubtotalAmount());
        order.setTaxAmount(cart.getTaxAmount());
        order.setShippingAmount(cart.getShippingAmount());
        order.setDiscountAmount(cart.getDiscountAmount());
        order.setTotalAmount(cart.getTotalAmount());
        order.setPlacedAt(OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime());
        order.setMetadataJson(cart.getMetadataJson());

        order = orderRepository.save(order);
        List<CommerceOrderItem> orderItems = buildOrderItems(order, cart.getItems());
        orderItemRepository.saveAll(orderItems);
        order.setItems(orderItems);

        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        return mapper.toOrderResponse(order);
    }

    private void addItemToCart(CommerceCart cart, CartLineItemRequest request) {
        CommerceProductVariant variant = variantRepository.findByCode(request.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + request.getVariantId()));

        if (!variant.getCurrencyCode().equalsIgnoreCase(cart.getCurrencyCode())) {
            throw new IllegalArgumentException("Cart currency does not match variant currency");
        }

        int quantity = Math.max(1, request.getQuantity());
        if (variant.getInventoryQuantity() != null && variant.getInventoryQuantity() < quantity) {
            throw new IllegalStateException("Insufficient inventory for variant " + variant.getCode());
        }

        CommerceCartItem existing = findExistingItem(cart, variant);
        if (existing == null) {
            CommerceCartItem item = new CommerceCartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(quantity);
            BigDecimal unitAmount = amount(variant.getUnitAmount());
            item.setUnitAmount(unitAmount);
            item.setSubtotalAmount(unitAmount.multiply(BigDecimal.valueOf(quantity)));
            item.setTotalAmount(item.getSubtotalAmount());
            item.setMetadataJson(writeMetadata(sanitizeMetadata(request.getMetadata())));

            if (cart.getItems() == null) {
                cart.setItems(new ArrayList<>());
            }
            cart.getItems().add(item);
            cartItemRepository.save(item);
        } else {
            int newQuantity = existing.getQuantity() + quantity;
            if (variant.getInventoryQuantity() != null && variant.getInventoryQuantity() < newQuantity) {
                throw new IllegalStateException("Insufficient inventory for variant " + variant.getCode());
            }
            existing.setQuantity(newQuantity);
            existing.setSubtotalAmount(existing.getUnitAmount().multiply(BigDecimal.valueOf(newQuantity)));
            existing.setTotalAmount(existing.getSubtotalAmount());
            cartItemRepository.save(existing);
        }
    }

    private CommerceCartItem findExistingItem(CommerceCart cart, CommerceProductVariant variant) {
        if (CollectionUtils.isEmpty(cart.getItems())) {
            return null;
        }
        return cart.getItems().stream()
                .filter(item -> item.getVariant() != null && Objects.equals(item.getVariant().getId(), variant.getId()))
                .findFirst()
                .orElse(null);
    }

    private void recalcTotals(CommerceCart cart) {
        BigDecimal subtotal = ZERO;
        if (!CollectionUtils.isEmpty(cart.getItems())) {
            subtotal = cart.getItems().stream()
                    .map(item -> item.getTotalAmount() == null ? ZERO : item.getTotalAmount())
                    .reduce(ZERO, BigDecimal::add);
        }
        cart.setSubtotalAmount(amount(subtotal));

        BigDecimal total = subtotal
                .add(cart.getTaxAmount() == null ? ZERO : cart.getTaxAmount())
                .add(cart.getShippingAmount() == null ? ZERO : cart.getShippingAmount())
                .subtract(cart.getDiscountAmount() == null ? ZERO : cart.getDiscountAmount());
        cart.setTotalAmount(amount(total));
    }

    private List<CommerceOrderItem> buildOrderItems(CommerceOrder order, List<CommerceCartItem> cartItems) {
        if (CollectionUtils.isEmpty(cartItems)) {
            return List.of();
        }
        List<CommerceOrderItem> items = new ArrayList<>();
        for (CommerceCartItem cartItem : cartItems) {
            CommerceOrderItem orderItem = new CommerceOrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitAmount(cartItem.getUnitAmount());
            orderItem.setSubtotalAmount(cartItem.getSubtotalAmount());
            orderItem.setTotalAmount(cartItem.getTotalAmount());
            orderItem.setTitle(cartItem.getVariant() != null ? cartItem.getVariant().getTitle() : null);
            orderItem.setMetadataJson(cartItem.getMetadataJson());
            items.add(orderItem);
        }
        return items;
    }

    private CommerceCart loadCart(String cartId) {
        UUID uuid = parseUuid(cartId);
        return cartRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cart identifier", ex);
        }
    }

    private void ensureOpen(CommerceCart cart) {
        if (cart.getStatus() != null && cart.getStatus() != CartStatus.OPEN) {
            throw new IllegalStateException("Cart is not open");
        }
    }

    private Map<String, Object> mergeMetadata(String existingJson, Map<String, Object> updates) {
        Map<String, Object> merged = new LinkedHashMap<>(readMetadata(existingJson));
        if (updates != null) {
            merged.putAll(sanitizeMetadata(updates));
        }
        return merged;
    }

    private Map<String, Object> readMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata;
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String resolveCustomerEmail(CommerceCart cart) {
        Map<String, Object> metadata = readMetadata(cart.getMetadataJson());
        Object email = metadata.get("customer_email");
        if (email instanceof String s && StringUtils.hasText(s)) {
            return s;
        }
        email = metadata.get("email");
        if (email instanceof String s2 && StringUtils.hasText(s2)) {
            return s2;
        }
        return null;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }
}
