package apps.sarafrika.elimika.commerce.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCheckoutRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.medusa.exception.MedusaIntegrationException;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MedusaOrderServiceTest {

    @Test
    void completeCheckout_updatesCartAndCompletesOrder() {
        MedusaCartService cartService = mock(MedusaCartService.class);
        MedusaOrderResponse orderResponse = new MedusaOrderResponse();
        orderResponse.setId("order_1");
        when(cartService.completeCart("cart_1")).thenReturn(orderResponse);

        MedusaOrderService service = new MedusaOrderService(cartService);

        MedusaCheckoutRequest request = MedusaCheckoutRequest.builder()
                .cartId("cart_1")
                .customerEmail("user@example.com")
                .shippingAddressId("ship_1")
                .billingAddressId("bill_1")
                .paymentProviderId("stripe")
                .build();

        MedusaOrderResponse result = service.completeCheckout(request);

        verify(cartService).updateCart("cart_1", Map.of(
                "email", "user@example.com",
                "shipping_address_id", "ship_1",
                "billing_address_id", "bill_1"));
        verify(cartService).selectPaymentSession("cart_1", "stripe");
        verify(cartService).completeCart("cart_1");
        assertThat(result.getId()).isEqualTo("order_1");
    }

    @Test
    void completeCheckout_withoutAddresses_onlySendsEmail() {
        MedusaCartService cartService = mock(MedusaCartService.class);
        when(cartService.completeCart("cart_2")).thenReturn(new MedusaOrderResponse());

        MedusaOrderService service = new MedusaOrderService(cartService);

        MedusaCheckoutRequest request = MedusaCheckoutRequest.builder()
                .cartId("cart_2")
                .customerEmail("user@example.com")
                .paymentProviderId("manual")
                .build();

        service.completeCheckout(request);

        verify(cartService).updateCart("cart_2", Map.of("email", "user@example.com"));
        verify(cartService).selectPaymentSession("cart_2", "manual");
        verify(cartService).completeCart("cart_2");
    }

    @Test
    void completeCheckout_whenRequestMissing_throws() {
        MedusaOrderService service = new MedusaOrderService(mock(MedusaCartService.class));

        assertThatThrownBy(() -> service.completeCheckout(null))
                .isInstanceOf(MedusaIntegrationException.class)
                .hasMessageContaining("Checkout request must be provided");
    }
}
