package apps.sarafrika.elimika.commerce.medusa.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.commerce.medusa.service.MedusaCustomerService;
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
public class MedusaCustomerServiceImpl implements MedusaCustomerService {

    private final RestClient medusaRestClient;

    public MedusaCustomerServiceImpl(RestClient medusaRestClient) {
        this.medusaRestClient = medusaRestClient;
    }

    @Override
    public MedusaCustomerResponse ensureCustomer(MedusaCustomerRequest request) {
        if (request == null || !StringUtils.hasText(request.getEmail())) {
            throw new MedusaIntegrationException("Customer email must be provided");
        }

        MedusaCustomerResponse existing = findCustomerByEmail(request.getEmail());
        if (existing != null) {
            return existing;
        }

        Map<String, Object> payload = buildCreationPayload(request);

        try {
            CustomerEnvelope envelope = medusaRestClient
                    .post()
                    .uri("/admin/customers")
                    .body(payload)
                    .retrieve()
                    .body(CustomerEnvelope.class);

            if (envelope == null || envelope.getCustomer() == null) {
                throw new MedusaIntegrationException("Medusa customer response was empty");
            }

            return envelope.getCustomer();
        } catch (RestClientResponseException ex) {
            throw new MedusaIntegrationException(
                    "Medusa rejected customer creation: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new MedusaIntegrationException("Failed to call Medusa customer API", ex);
        }
    }

    private MedusaCustomerResponse findCustomerByEmail(String email) {
        try {
            CustomerListEnvelope envelope = medusaRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/customers")
                            .queryParam("limit", 1)
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .body(CustomerListEnvelope.class);

            if (envelope != null && !CollectionUtils.isEmpty(envelope.getCustomers())) {
                return envelope.getCustomers().get(0);
            }
            return null;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return null;
            }
            throw new MedusaIntegrationException(
                    "Medusa rejected customer lookup: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new MedusaIntegrationException("Failed to call Medusa customer API", ex);
        }
    }

    private Map<String, Object> buildCreationPayload(MedusaCustomerRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", request.getEmail());
        if (StringUtils.hasText(request.getFirstName())) {
            payload.put("first_name", request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            payload.put("last_name", request.getLastName());
        }
        return payload;
    }

    @Getter
    private static class CustomerEnvelope {
        private MedusaCustomerResponse customer;
    }

    @Getter
    private static class CustomerListEnvelope {
        private List<MedusaCustomerResponse> customers;
    }
}
