package apps.sarafrika.elimika.commerce.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaLineItemRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class MedusaCartServiceTest {

    private static final String BASE_URL = "https://medusa.example.com";

    @Test
    void createCart_withAllFields_buildsExpectedPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        MedusaCartRequest request = MedusaCartRequest.builder()
                .regionId("reg_1")
                .customerId("cus_1")
                .salesChannelId("channel_1")
                .metadata(Map.of("source", "elimika"))
                .items(List.of(MedusaLineItemRequest.builder()
                        .variantId("var_1")
                        .quantity(2)
                        .build()))
                .build();

        String expectedBody = """
                {
                  "region_id": "reg_1",
                  "customer_id": "cus_1",
                  "sales_channel_id": "channel_1",
                  "metadata": {"source": "elimika"},
                  "items": [
                    {"variant_id": "var_1", "quantity": 2}
                  ]
                }
                """;

        String responseBody = """
                {
                  "cart": {
                    "id": "cart_1",
                    "region_id": "reg_1",
                    "customer_id": "cus_1"
                  }
                }
                """;

        server.expect(requestTo(BASE_URL + "/store/carts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedBody, true))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.createCart(request);

        assertThat(cart.getId()).isEqualTo("cart_1");
        server.verify();
    }

    @Test
    void createCart_withoutOptionalFields_excludesValues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        MedusaCartRequest request = MedusaCartRequest.builder()
                .regionId("reg_1")
                .build();

        server.expect(requestTo(BASE_URL + "/store/carts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"region_id": "reg_1"}
                        """, true))
                .andRespond(withSuccess("""
                        {"cart": {"id": "cart_min"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.createCart(request);
        assertThat(cart.getId()).isEqualTo("cart_min");
        server.verify();
    }

    @Test
    void createCart_whenResponseMissingCart_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts"))
                .andRespond(withSuccess("""
                        {"cart": null}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.createCart(MedusaCartRequest.builder()
                        .regionId("reg")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa cart response was empty");

        server.verify();
    }

    @Test
    void createCart_whenMedusaRejectsRequest_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts"))
                .andRespond(withBadRequest().body("{\"error\":true}"));

        assertThatThrownBy(() -> service.createCart(MedusaCartRequest.builder()
                        .regionId("reg")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa rejected cart request");

        server.verify();
    }

    @Test
    void createCart_whenClientFails_throws() {
        RestClient restClient = mock(RestClient.class);
        when(restClient.post()).thenThrow(new RestClientException("network error"));

        MedusaCartService service = new MedusaCartService(restClient);

        assertThatThrownBy(() -> service.createCart(MedusaCartRequest.builder()
                        .regionId("reg")
                        .build()))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Failed to call Medusa cart API");
    }

    @Test
    void addItemToCart_addsVariant() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1/line-items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"variant_id": "var_2", "quantity": 3}
                        """, true))
                .andRespond(withSuccess("""
                        {"cart": {"id": "cart_1"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.addItemToCart("cart_1", MedusaLineItemRequest.builder()
                .variantId("var_2")
                .quantity(3)
                .build());

        assertThat(cart.getId()).isEqualTo("cart_1");
        server.verify();
    }

    @Test
    void retrieveCart_returnsDetails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"cart": {"id": "cart_1", "items": []}}
                        """, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.retrieveCart("cart_1");
        assertThat(cart.getId()).isEqualTo("cart_1");
        server.verify();
    }

    @Test
    void updateCart_appliesUpdates() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"email": "user@example.com"}
                        """, true))
                .andRespond(withSuccess("""
                        {"cart": {"id": "cart_1", "customer_id": "cus_9"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.updateCart("cart_1", Map.of("email", "user@example.com"));
        assertThat(cart.getCustomerId()).isEqualTo("cus_9");
        server.verify();
    }

    @Test
    void selectPaymentSession_setsProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1/payment-session"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"provider_id": "stripe"}
                        """, true))
                .andRespond(withSuccess("""
                        {"cart": {"id": "cart_1"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaCartResponse cart = service.selectPaymentSession("cart_1", "stripe");
        assertThat(cart.getId()).isEqualTo("cart_1");
        server.verify();
    }

    @Test
    void completeCart_returnsOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1/complete"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"order": {"id": "order_1", "status": "completed"}}
                        """, MediaType.APPLICATION_JSON));

        MedusaOrderResponse order = service.completeCart("cart_1");
        assertThat(order.getId()).isEqualTo("order_1");
        server.verify();
    }

    @Test
    void completeCart_whenResponseMissingOrder_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1/complete"))
                .andRespond(withSuccess("""
                        {"order": null}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.completeCart("cart_1"))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa order response was empty");

        server.verify();
    }

    @Test
    void completeCart_whenMedusaRejectsRequest_throws() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        MedusaCartService service = new MedusaCartService(restClient);

        server.expect(requestTo(BASE_URL + "/store/carts/cart_1/complete"))
                .andRespond(withBadRequest().body("{\"error\":true}"));

        assertThatThrownBy(() -> service.completeCart("cart_1"))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Medusa rejected cart completion");

        server.verify();
    }

    @Test
    void completeCart_whenClientFails_throws() {
        RestClient restClient = mock(RestClient.class);
        when(restClient.post()).thenThrow(new RestClientException("network error"));

        MedusaCartService service = new MedusaCartService(restClient);

        assertThatThrownBy(() -> service.completeCart("cart_1"))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Failed to call Medusa checkout API");
    }
}
