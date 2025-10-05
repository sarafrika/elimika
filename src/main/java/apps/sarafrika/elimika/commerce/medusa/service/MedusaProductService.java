package apps.sarafrika.elimika.commerce.medusa.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;

import lombok.Getter;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Application service that encapsulates calls to the Medusa Admin API for product management.
 */
@Service
public class MedusaProductService {

    private final RestClient medusaRestClient;

    public MedusaProductService(RestClient medusaRestClient) {
        this.medusaRestClient = medusaRestClient;
    }

    /**
     * Creates a digital product with a single SKU on the Medusa admin API.
     *
     * @param request the product definition to create
     * @return the persisted Medusa product
     */
    public MedusaDigitalProductResponse createDigitalProduct(MedusaDigitalProductRequest request) {
        Map<String, Object> payload = buildProductPayload(request);

        try {
            ProductEnvelope envelope = medusaRestClient
                    .post()
                    .uri("/admin/products")
                    .body(payload)
                    .retrieve()
                    .body(ProductEnvelope.class);

            if (envelope == null || envelope.product == null) {
                throw new MedusaIntegrationException("Medusa product response was empty");
            }

            return envelope.product;
        } catch (RestClientResponseException ex) {
            throw new MedusaIntegrationException(
                    "Medusa rejected product creation: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new MedusaIntegrationException("Failed to call Medusa product API", ex);
        }
    }

    private Map<String, Object> buildProductPayload(MedusaDigitalProductRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", request.getTitle());
        if (StringUtils.hasText(request.getSubtitle())) {
            payload.put("subtitle", request.getSubtitle());
        }
        if (StringUtils.hasText(request.getDescription())) {
            payload.put("description", request.getDescription());
        }
        payload.put("status", "published");
        payload.put("requires_shipping", request.isRequiresShipping());
        payload.put("is_giftcard", false);
        payload.put("options", List.of(Map.of("title", request.getOptionTitle())));
        if (!CollectionUtils.isEmpty(request.getCollectionIds())) {
            payload.put("collection_ids", request.getCollectionIds());
        }
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            payload.put("metadata", request.getMetadata());
        }

        Map<String, Object> variant = new LinkedHashMap<>();
        String variantTitle = StringUtils.hasText(request.getVariantTitle())
                ? request.getVariantTitle()
                : request.getTitle();
        variant.put("title", variantTitle);
        variant.put("sku", request.getSku());
        variant.put("manage_inventory", false);
        variant.put("allow_backorder", true);
        variant.put("inventory_quantity", 0);
        variant.put(
                "prices",
                List.of(Map.of("amount", request.getAmount(), "currency_code", request.getCurrencyCode())));
        variant.put("options", List.of(Map.of("value", request.getOptionValue())));

        payload.put("variants", List.of(variant));
        return payload;
    }

    @Getter
    private static class ProductEnvelope {
        private MedusaDigitalProductResponse product;
    }
}
