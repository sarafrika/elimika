package apps.sarafrika.elimika.commerce.internal.mapper;

import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCart;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCartItem;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrderItem;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Maps internal commerce entities to API-facing DTOs.
 */
@Component
@RequiredArgsConstructor
public class InternalCommerceMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CartResponse toCartResponse(CommerceCart cart) {
        if (cart == null) {
            return null;
        }
        return CartResponse.builder()
                .id(optionalUuid(cart.getUuid()))
                .currencyCode(cart.getCurrencyCode())
                .regionCode(cart.getRegionCode())
                .status(cart.getStatus() == null ? null : cart.getStatus().name())
                .subtotal(amount(cart.getSubtotalAmount()))
                .tax(amount(cart.getTaxAmount()))
                .discount(amount(cart.getDiscountAmount()))
                .shipping(amount(cart.getShippingAmount()))
                .total(amount(cart.getTotalAmount()))
                .createdAt(toOffset(cart.getCreatedDate()))
                .updatedAt(toOffset(cart.getLastModifiedDate()))
                .items(toCartItems(cart.getItems()))
                .build();
    }

    public OrderResponse toOrderResponse(CommerceOrder order) {
        if (order == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(optionalUuid(order.getUuid()))
                .displayId(order.getId() == null ? null : order.getId().toString())
                .paymentStatus(order.getPaymentStatus() == null ? null : order.getPaymentStatus().name())
                .currencyCode(order.getCurrencyCode())
                .subtotal(amount(order.getSubtotalAmount()))
                .total(amount(order.getTotalAmount()))
                .createdAt(toOffset(order.getPlacedAt()))
                .items(toOrderItems(order.getItems()))
                .build();
    }

    private List<CartItemResponse> toCartItems(List<CommerceCartItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> CartItemResponse.builder()
                        .id(optionalUuid(item.getUuid()))
                        .title(Optional.ofNullable(item.getVariant()).map(v -> v.getTitle()).orElse(null))
                        .quantity(item.getQuantity() == null ? 0 : item.getQuantity())
                        .variantId(Optional.ofNullable(item.getVariant()).map(v -> v.getCode()).orElse(null))
                        .unitPrice(item.getUnitAmount())
                        .subtotal(item.getSubtotalAmount())
                        .total(item.getTotalAmount())
                        .metadata(readMetadata(item.getMetadataJson()))
                        .build())
                .toList();
    }

    private List<CartItemResponse> toOrderItems(List<CommerceOrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> CartItemResponse.builder()
                        .id(optionalUuid(item.getUuid()))
                        .title(item.getTitle())
                        .quantity(item.getQuantity() == null ? 0 : item.getQuantity())
                        .variantId(Optional.ofNullable(item.getVariant()).map(v -> v.getCode()).orElse(null))
                        .unitPrice(item.getUnitAmount())
                        .subtotal(item.getSubtotalAmount())
                        .total(item.getTotalAmount())
                        .metadata(readMetadata(item.getMetadataJson()))
                        .build())
                .toList();
    }

    private Map<String, Object> readMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (IOException ex) {
            return Map.of();
        }
    }

    private OffsetDateTime toOffset(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
    }

    private String optionalUuid(java.util.UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? null : value.setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
