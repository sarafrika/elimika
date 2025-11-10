package apps.sarafrika.elimika.commerce.shared.mapper;

import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
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
                .regionId(medusaCart.getRegionId())
                .customerId(medusaCart.getCustomerId())
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
                .subtotal(medusaOrder.getSubtotal())
                .total(medusaOrder.getTotal())
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
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .total(item.getTotal())
                        .metadata(item.getMetadata())
                        .build())
                .toList();
    }
}
