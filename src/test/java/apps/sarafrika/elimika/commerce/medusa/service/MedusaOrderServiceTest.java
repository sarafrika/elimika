package apps.sarafrika.elimika.commerce.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerResponse;
import apps.sarafrika.elimika.commerce.medusa.service.impl.MedusaOrderServiceImpl;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class MedusaOrderServiceTest {

    @Test
    void completeCheckout_updatesCartAndCompletesOrder() {
        MedusaCartService cartService = mock(MedusaCartService.class);
        MedusaCustomerService customerService = mock(MedusaCustomerService.class);
        MedusaOrderResponse orderResponse = new MedusaOrderResponse();
        orderResponse.setId("order_1");
        when(cartService.completeCart("cart_1")).thenReturn(orderResponse);
        when(customerService.ensureCustomer(any(MedusaCustomerRequest.class)))
                .thenReturn(buildCustomer());

        MedusaOrderService service = new MedusaOrderServiceImpl(cartService, customerService, mock(RestClient.class));

        MedusaCheckoutRequest request = MedusaCheckoutRequest.builder()
                .cartId("cart_1")
                .customerEmail("user@example.com")
                .shippingAddressId("ship_1")
                .billingAddressId("bill_1")
                .paymentProviderId("stripe")
                .build();

        MedusaOrderResponse result = service.completeCheckout(request);

        verify(customerService).ensureCustomer(argThat(customerRequest ->
                "user@example.com".equals(customerRequest.getEmail())));
        verify(cartService).updateCart("cart_1", Map.of(
                "email", "user@example.com",
                "customer_id", "cus_123",
                "shipping_address_id", "ship_1",
                "billing_address_id", "bill_1"));
        verify(cartService).selectPaymentSession("cart_1", "stripe");
        verify(cartService).completeCart("cart_1");
        assertThat(result.getId()).isEqualTo("order_1");
    }

    @Test
    void completeCheckout_withoutAddresses_onlySendsEmail() {
        MedusaCartService cartService = mock(MedusaCartService.class);
        MedusaCustomerService customerService = mock(MedusaCustomerService.class);
        when(cartService.completeCart("cart_2")).thenReturn(new MedusaOrderResponse());
        when(customerService.ensureCustomer(any(MedusaCustomerRequest.class)))
                .thenReturn(buildCustomer());

        MedusaOrderService service = new MedusaOrderServiceImpl(cartService, customerService, mock(RestClient.class));

        MedusaCheckoutRequest request = MedusaCheckoutRequest.builder()
                .cartId("cart_2")
                .customerEmail("user@example.com")
                .paymentProviderId("manual")
                .build();

        service.completeCheckout(request);

        verify(customerService).ensureCustomer(argThat(customerRequest ->
                "user@example.com".equals(customerRequest.getEmail())));
        verify(cartService).updateCart("cart_2", Map.of(
                "email", "user@example.com",
                "customer_id", "cus_123"));
        verify(cartService).selectPaymentSession("cart_2", "manual");
        verify(cartService).completeCart("cart_2");
    }

    @Test
    void completeCheckout_whenRequestMissing_throws() {
        MedusaOrderService service =
                new MedusaOrderServiceImpl(
                        mock(MedusaCartService.class),
                        mock(MedusaCustomerService.class),
                        mock(RestClient.class));

        assertThatThrownBy(() -> service.completeCheckout(null))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Checkout request must be provided");
    }

    @SuppressWarnings("unchecked")
    @Test
    void retrieveOrder_fetchesOrderFromMedusa() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/store/orders/{id}"), eq("order_1"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        MedusaOrderServiceImpl.OrderEnvelope envelope = new MedusaOrderServiceImpl.OrderEnvelope();
        MedusaOrderResponse medusaOrder = new MedusaOrderResponse();
        medusaOrder.setId("order_1");
        envelope.setOrder(medusaOrder);

        when(responseSpec.body(any(Class.class))).thenReturn(envelope);

        MedusaOrderService service = new MedusaOrderServiceImpl(
                mock(MedusaCartService.class),
                mock(MedusaCustomerService.class),
                restClient);

        MedusaOrderResponse result = service.retrieveOrder("order_1");

        assertThat(result.getId()).isEqualTo("order_1");
        verify(uriSpec).uri("/store/orders/{id}", "order_1");
    }

    private MedusaCustomerResponse buildCustomer() {
        MedusaCustomerResponse response = new MedusaCustomerResponse();
        response.setId("cus_123");
        response.setEmail("user@example.com");
        return response;
    }
}
