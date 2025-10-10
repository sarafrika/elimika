package apps.sarafrika.elimika.commerce.cart.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CreateCartRequest;
import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.cart.service.CartService;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaLineItemRequest;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaCartService;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;
import apps.sarafrika.elimika.commerce.shared.mapper.MedusaCommerceMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link CartService} delegating to the Medusa cart APIs.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final MedusaCartService medusaCartService;
    private final MedusaCommerceMapper mapper;

    @Override
    public CartResponse createCart(CreateCartRequest request) {
        MedusaCartRequest medusaRequest = MedusaCartRequest.builder()
                .regionId(request.getRegionId())
                .customerId(request.getCustomerId())
                .salesChannelId(request.getSalesChannelId())
                .metadata(request.getMetadata() == null ? Map.of() : request.getMetadata())
                .items(toMedusaLineItems(request.getItems()))
                .build();
        return mapper.toCartResponse(medusaCartService.createCart(medusaRequest));
    }

    @Override
    public CartResponse addItem(String cartId, CartLineItemRequest request) {
        return mapper.toCartResponse(medusaCartService.addItemToCart(cartId, MedusaLineItemRequest.builder()
                .variantId(request.getVariantId())
                .quantity(request.getQuantity())
                .metadata(sanitizeMetadata(request.getMetadata()))
                .build()));
    }

    @Override
    public CartResponse getCart(String cartId) {
        return mapper.toCartResponse(medusaCartService.retrieveCart(cartId));
    }

    @Override
    public CartResponse updateCart(String cartId, UpdateCartRequest request) {
        Map<String, Object> updates = buildUpdatePayload(request);
        if (updates.isEmpty()) {
            return getCart(cartId);
        }
        return mapper.toCartResponse(medusaCartService.updateCart(cartId, updates));
    }

    @Override
    public CartResponse selectPaymentSession(String cartId, SelectPaymentSessionRequest request) {
        return mapper.toCartResponse(medusaCartService.selectPaymentSession(cartId, request.getProviderId()));
    }

    @Override
    public OrderResponse completeCart(String cartId) {
        return mapper.toOrderResponse(medusaCartService.completeCart(cartId));
    }

    private List<MedusaLineItemRequest> toMedusaLineItems(List<CartLineItemRequest> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> MedusaLineItemRequest.builder()
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .metadata(sanitizeMetadata(item.getMetadata()))
                        .build())
                .toList();
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata;
    }

    private Map<String, Object> buildUpdatePayload(UpdateCartRequest request) {
        Map<String, Object> updates = new LinkedHashMap<>();
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getCustomerId())
                && !StringUtils.hasText(request.getShippingAddressId()) && !StringUtils.hasText(request.getBillingAddressId())
                && (request.getMetadata() == null || request.getMetadata().isEmpty())) {
            return updates;
        }

        if (StringUtils.hasText(request.getEmail())) {
            updates.put("email", request.getEmail());
        }
        if (StringUtils.hasText(request.getCustomerId())) {
            updates.put("customer_id", request.getCustomerId());
        }
        if (StringUtils.hasText(request.getShippingAddressId())) {
            updates.put("shipping_address_id", request.getShippingAddressId());
        }
        if (StringUtils.hasText(request.getBillingAddressId())) {
            updates.put("billing_address_id", request.getBillingAddressId());
        }
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            updates.put("metadata", request.getMetadata());
        }
        return updates;
    }
}
