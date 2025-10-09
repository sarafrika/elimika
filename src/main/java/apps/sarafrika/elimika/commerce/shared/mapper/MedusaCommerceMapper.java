package apps.sarafrika.elimika.commerce.shared.mapper;

import apps.sarafrika.elimika.commerce.cart.dto.CartItemResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
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
        if (medusaOrder == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(medusaOrder.getId())
                .displayId(medusaOrder.getDisplayId())
                .status(medusaOrder.getStatus())
                .fulfillmentStatus(medusaOrder.getFulfillmentStatus())
                .paymentStatus(medusaOrder.getPaymentStatus())
                .createdAt(medusaOrder.getCreatedAt())
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
                        .build())
                .toList();
    }
}
