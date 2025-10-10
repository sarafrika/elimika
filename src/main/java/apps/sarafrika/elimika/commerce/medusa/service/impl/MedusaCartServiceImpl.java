package apps.sarafrika.elimika.commerce.medusa.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaLineItemRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaCartService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MedusaCartServiceImpl implements MedusaCartService {

    private final RestClient medusaRestClient;

    public MedusaCartServiceImpl(RestClient medusaRestClient) {
        this.medusaRestClient = medusaRestClient;
    }

    @Override
    public MedusaCartResponse createCart(MedusaCartRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("region_id", request.getRegionId());
        if (StringUtils.hasText(request.getCustomerId())) {
            payload.put("customer_id", request.getCustomerId());
        }
        if (StringUtils.hasText(request.getSalesChannelId())) {
            payload.put("sales_channel_id", request.getSalesChannelId());
        }
        if (!CollectionUtils.isEmpty(request.getItems())) {
            payload.put("items", toLineItems(request.getItems()));
        }
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            payload.put("metadata", request.getMetadata());
        }

        return executeCartCall(() -> medusaRestClient
                .post()
                .uri("/store/carts")
                .body(payload)
                .retrieve()
                .body(CartEnvelope.class));
    }

    @Override
    public MedusaCartResponse addItemToCart(String cartId, MedusaLineItemRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("variant_id", request.getVariantId());
        payload.put("quantity", request.getQuantity());
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            payload.put("metadata", request.getMetadata());
        }

        return executeCartCall(() -> medusaRestClient
                .post()
                .uri("/store/carts/{id}/line-items", cartId)
                .body(payload)
                .retrieve()
                .body(CartEnvelope.class));
    }

    @Override
    public MedusaCartResponse retrieveCart(String cartId) {
        return executeCartCall(() -> medusaRestClient
                .get()
                .uri("/store/carts/{id}", cartId)
                .retrieve()
                .body(CartEnvelope.class));
    }

    @Override
    public MedusaCartResponse updateCart(String cartId, Map<String, Object> updates) {
        return executeCartCall(() -> medusaRestClient
                .post()
                .uri("/store/carts/{id}", cartId)
                .body(updates)
                .retrieve()
                .body(CartEnvelope.class));
    }

    @Override
    public MedusaCartResponse selectPaymentSession(String cartId, String providerId) {
        Map<String, Object> payload = Map.of("provider_id", providerId);

        return executeCartCall(() -> medusaRestClient
                .post()
                .uri("/store/carts/{id}/payment-session", cartId)
                .body(payload)
                .retrieve()
                .body(CartEnvelope.class));
    }

    @Override
    public MedusaOrderResponse completeCart(String cartId) {
        try {
            OrderEnvelope envelope = medusaRestClient
                    .post()
                    .uri("/store/carts/{id}/complete", cartId)
                    .body(Map.of())
                    .retrieve()
                    .body(OrderEnvelope.class);
            if (envelope == null || envelope.getOrder() == null) {
                throw new MedusaIntegrationException("Medusa order response was empty");
            }
            return envelope.getOrder();
        } catch (RestClientResponseException ex) {
            throw new MedusaIntegrationException(
                    "Medusa rejected cart completion: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new MedusaIntegrationException("Failed to call Medusa checkout API", ex);
        }
    }

    private List<Map<String, Object>> toLineItems(List<MedusaLineItemRequest> items) {
        return items.stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("variant_id", item.getVariantId());
                    map.put("quantity", item.getQuantity());
                    if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                        map.put("metadata", item.getMetadata());
                    }
                    return map;
                })
                .toList();
    }

    private MedusaCartResponse executeCartCall(CartSupplier supplier) {
        try {
            CartEnvelope envelope = supplier.get();
            if (envelope == null || envelope.getCart() == null) {
                throw new MedusaIntegrationException("Medusa cart response was empty");
            }
            return envelope.getCart();
        } catch (RestClientResponseException ex) {
            throw new MedusaIntegrationException(
                    "Medusa rejected cart request: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new MedusaIntegrationException("Failed to call Medusa cart API", ex);
        }
    }

    @FunctionalInterface
    private interface CartSupplier {
        CartEnvelope get();
    }

    @Getter
    private static class CartEnvelope {
        private MedusaCartResponse cart;
    }

    @Getter
    private static class OrderEnvelope {
        private MedusaOrderResponse order;
    }
}
