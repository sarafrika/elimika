package apps.sarafrika.elimika.commerce.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.commerce.medusa.service.impl.MedusaCustomerServiceImpl;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

class MedusaCustomerServiceTest {

    private static final String BASE_URL = "https://medusa.example.com";

    @Test
    void ensureCustomer_whenCustomerExists_returnsExistingRecord() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        String lookupResponse = """
                {
                  "customers": [
                    {
                      "id": "cus_123",
                      "email": "user@example.com",
                      "created_at": "2024-01-01T00:00:00Z"
                    }
                  ]
                }
                """;

        server.expect(customerLookupRequest("user@example.com"))
                .andRespond(withSuccess(lookupResponse, MediaType.APPLICATION_JSON));

        MedusaCustomerResponse response = service.ensureCustomer(MedusaCustomerRequest.builder()
                .email("user@example.com")
                .build());

        assertThat(response.getId()).isEqualTo("cus_123");
        assertThat(response.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        server.verify();
    }

    @Test
    void ensureCustomer_whenCustomerMissing_createsNewRecord() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        server.expect(customerLookupRequest("new@example.com"))
                .andRespond(withSuccess("""
                        {"customers": []}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE_URL + "/admin/customers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "email": "new@example.com",
                          "first_name": "New",
                          "last_name": "User"
                        }
                        """, true))
                .andRespond(withSuccess("""
                        {
                          "customer": {
                            "id": "cus_new",
                            "email": "new@example.com"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MedusaCustomerResponse response = service.ensureCustomer(MedusaCustomerRequest.builder()
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .build());

        assertThat(response.getId()).isEqualTo("cus_new");
        server.verify();
    }

    @Test
    void ensureCustomer_whenLookupReturns404_stillCreatesCustomer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        server.expect(customerLookupRequest("missing@example.com"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(requestTo(BASE_URL + "/admin/customers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"email": "missing@example.com"}
                        """, true))
                .andRespond(withSuccess("""
                        {
                          "customer": {
                            "id": "cus_created",
                            "email": "missing@example.com"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MedusaCustomerResponse response = service.ensureCustomer(MedusaCustomerRequest.builder()
                .email("missing@example.com")
                .build());

        assertThat(response.getId()).isEqualTo("cus_created");
        server.verify();
    }

    @Test
    void ensureCustomer_whenCreationResponseMissingCustomer_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        server.expect(customerLookupRequest("error@example.com"))
                .andRespond(withSuccess("""
                        {"customers": []}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE_URL + "/admin/customers"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "customer": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.ensureCustomer(MedusaCustomerRequest.builder()
                        .email("error@example.com")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa customer response was empty");
        server.verify();
    }

    @Test
    void ensureCustomer_whenLookupFails_throwsIntegrationException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        server.expect(customerLookupRequest("boom@example.com"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.ensureCustomer(MedusaCustomerRequest.builder()
                        .email("boom@example.com")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa rejected customer lookup");
        server.verify();
    }

    @Test
    void ensureCustomer_whenCreationRejected_throwsIntegrationException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCustomerService service = new MedusaCustomerServiceImpl(restClient);

        server.expect(customerLookupRequest("reject@example.com"))
                .andRespond(withSuccess("""
                        {"customers": []}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE_URL + "/admin/customers"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> service.ensureCustomer(MedusaCustomerRequest.builder()
                        .email("reject@example.com")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa rejected customer creation");
        server.verify();
    }

    @Test
    void ensureCustomer_whenEmailMissing_throwsValidationError() {
        MedusaCustomerService service = new MedusaCustomerServiceImpl(RestClient.builder().baseUrl(BASE_URL).build());

        assertThatThrownBy(() -> service.ensureCustomer(MedusaCustomerRequest.builder().build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Customer email must be provided");
}

    private RequestMatcher customerLookupRequest(String email) {
        return request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI().getPath()).isEqualTo("/admin/customers");
            String query = request.getURI().getQuery();
            assertThat(query).contains("limit=1");
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            boolean matchesEncoded = query.contains("email=" + encodedEmail);
            boolean matchesRaw = query.contains("email=" + email);
            assertThat(matchesEncoded || matchesRaw)
                    .as("email query parameter to match %s", email)
                    .isTrue();
        };
    }
}
