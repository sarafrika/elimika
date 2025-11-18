package apps.sarafrika.elimika.commerce.shared.mapper;

import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Maps Medusa DTOs into API facing commerce DTOs.
 */
@Component
public class MedusaCommerceMapper {

    public CartResponse toCartResponse(MedusaCartResponse medusaCart) {
        if (medusaCart == null) {
            return null;
        }
        return CartResponse.builder()
                .id(medusaCart.getId())
                .currencyCode(null)
                .regionCode(medusaCart.getRegionId())
                .status(null)
                .subtotal(null)
                .tax(null)
                .discount(null)
                .shipping(null)
                .total(null)
                .createdAt(medusaCart.getCreatedAt())
                .updatedAt(medusaCart.getUpdatedAt())
                .items(toCartItems(medusaCart.getItems()))
                .build();
    }

    public OrderResponse toOrderResponse(MedusaOrderResponse medusaOrder) {
        return toOrderResponse(medusaOrder, null);
    }

    public OrderResponse toOrderResponse(MedusaOrderResponse medusaOrder, PlatformFeeBreakdown platformFee) {
        if (medusaOrder == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(medusaOrder.getId())
                .displayId(medusaOrder.getDisplayId())
                .paymentStatus(medusaOrder.getPaymentStatus())
                .createdAt(medusaOrder.getCreatedAt())
                .currencyCode(medusaOrder.getCurrencyCode())
                .subtotal(toAmount(medusaOrder.getSubtotal()))
                .total(toAmount(medusaOrder.getTotal()))
                .platformFee(platformFee)
                .items(toCartItems(medusaOrder.getItems()))
                .build();
    }

    private List<CartItemResponse> toCartItems(List<MedusaCartResponse.MedusaLineItemResponse> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .quantity(item.getQuantity())
                        .variantId(item.getVariantId())
                        .unitPrice(toAmount(item.getUnitPrice()))
                        .subtotal(toAmount(item.getSubtotal()))
                        .total(toAmount(item.getTotal()))
                        .metadata(item.getMetadata())
                        .build())
                .toList();
    }

    private BigDecimal toAmount(Long value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP);
    }
}
