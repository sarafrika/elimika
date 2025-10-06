package apps.sarafrika.elimika.commerce.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import apps.sarafrika.elimika.commerce.medusa.service.impl.MedusaProductServiceImpl;

class MedusaProductServiceTest {

    private static final String BASE_URL = "https://medusa.example.com";

    @Test
    void createDigitalProduct_withAllFields_sendsExpectedPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaProductService service = new MedusaProductServiceImpl(restClient);

        MedusaDigitalProductRequest request = MedusaDigitalProductRequest.builder()
                .title("Pro Course")
                .subtitle("Advanced skills")
                .description("Deep dive into tooling")
                .sku("SKU-123")
                .currencyCode("USD")
                .amount(9900)
                .requiresShipping(false)
                .variantTitle("Course License")
                .optionTitle("Delivery")
                .optionValue("Download")
                .metadata(Map.of("level", "expert"))
                .collectionIds(List.of("col_1"))
                .build();

        String expectedBody = """
                {
                  "title": "Pro Course",
                  "subtitle": "Advanced skills",
                  "description": "Deep dive into tooling",
                  "status": "published",
                  "requires_shipping": false,
                  "is_giftcard": false,
                  "options": [
                    {"title": "Delivery"}
                  ],
                  "collection_ids": ["col_1"],
                  "metadata": {"level": "expert"},
                  "variants": [
                    {
                      "title": "Course License",
                      "sku": "SKU-123",
                      "manage_inventory": false,
                      "allow_backorder": true,
                      "inventory_quantity": 0,
                      "prices": [
                        {"amount": 9900, "currency_code": "USD"}
                      ],
                      "options": [
                        {"value": "Download"}
                      ]
                    }
                  ]
                }
                """;

        String responseBody = """
                {
                  "product": {
                    "id": "prod_123",
                    "title": "Pro Course",
                    "handle": "pro-course",
                    "created_at": "2024-01-01T00:00:00Z",
                    "updated_at": "2024-01-02T00:00:00Z",
                    "variants": [
                      {
                        "id": "variant_1",
                        "title": "Course License",
                        "sku": "SKU-123",
                        "manage_inventory": false,
                        "allow_backorder": true
                      }
                    ]
                  }
                }
                """;

        server.expect(requestTo(BASE_URL + "/admin/products"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedBody, true))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        MedusaDigitalProductResponse response = service.createDigitalProduct(request);

        assertThat(response.getId()).isEqualTo("prod_123");
        assertThat(response.getVariants()).hasSize(1);
        assertThat(response.getVariants().get(0).getSku()).isEqualTo("SKU-123");
        assertThat(response.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        server.verify();
    }

    @Test
    void createDigitalProduct_withoutOptionalFields_usesFallbacks() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaProductService service = new MedusaProductServiceImpl(restClient);

        MedusaDigitalProductRequest request = MedusaDigitalProductRequest.builder()
                .title("Starter Course")
                .sku("SKU-START")
                .currencyCode("EUR")
                .amount(1500)
                .build();

        String expectedBody = """
                {
                  "title": "Starter Course",
                  "status": "published",
                  "requires_shipping": false,
                  "is_giftcard": false,
                  "options": [
                    {"title": "Format"}
                  ],
                  "variants": [
                    {
                      "title": "Starter Course",
                      "sku": "SKU-START",
                      "manage_inventory": false,
                      "allow_backorder": true,
                      "inventory_quantity": 0,
                      "prices": [
                        {"amount": 1500, "currency_code": "EUR"}
                      ],
                      "options": [
                        {"value": "Digital"}
                      ]
                    }
                  ]
                }
                """;

        server.expect(requestTo(BASE_URL + "/admin/products"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedBody, true))
                .andRespond(withSuccess("""
                        {"product": {"id": "prod_start", "title": "Starter Course"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaDigitalProductResponse response = service.createDigitalProduct(request);

        assertThat(response.getId()).isEqualTo("prod_start");
        server.verify();
    }

    @Test
    void createDigitalProduct_whenMedusaReturnsEmptyBody_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaProductService service = new MedusaProductServiceImpl(restClient);

        server.expect(requestTo(BASE_URL + "/admin/products"))
                .andRespond(withSuccess("""
                        {"product": null}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.createDigitalProduct(MedusaDigitalProductRequest.builder()
                        .title("Empty")
                        .sku("SKU-EMPTY")
                        .currencyCode("USD")
                        .amount(100)
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa product response was empty");

        server.verify();
    }

    @Test
    void createDigitalProduct_whenMedusaRejectsRequest_throwsException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaProductService service = new MedusaProductServiceImpl(restClient);

        server.expect(requestTo(BASE_URL + "/admin/products"))
                .andRespond(withBadRequest().body("{\"error\":\"invalid\"}"));

        assertThatThrownBy(() -> service.createDigitalProduct(MedusaDigitalProductRequest.builder()
                        .title("Invalid")
                        .sku("BAD")
                        .currencyCode("USD")
                        .amount(10)
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa rejected product creation");

        server.verify();
    }

    @Test
    void createDigitalProduct_whenClientFails_throwsException() {
        RestClient restClient = org.mockito.Mockito.mock(RestClient.class);
        org.mockito.Mockito.when(restClient.post()).thenThrow(new RestClientException("connection error"));

        MedusaProductService service = new MedusaProductServiceImpl(restClient);

        assertThatThrownBy(() -> service.createDigitalProduct(MedusaDigitalProductRequest.builder()
                        .title("Fail")
                        .sku("FAIL")
                        .currencyCode("USD")
                        .amount(100)
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Failed to call Medusa product API");
    }
}
