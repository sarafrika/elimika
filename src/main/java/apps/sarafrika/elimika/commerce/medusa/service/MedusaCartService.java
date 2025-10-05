package apps.sarafrika.elimika.commerce.medusa.service;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCartResponse;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaLineItemRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import java.util.Map;

/**
 * Encapsulates Medusa Store API calls related to carts and checkout orchestration.
 */
public interface MedusaCartService {

    MedusaCartResponse createCart(MedusaCartRequest request);

    MedusaCartResponse addItemToCart(String cartId, MedusaLineItemRequest request);

    MedusaCartResponse retrieveCart(String cartId);

    MedusaCartResponse updateCart(String cartId, Map<String, Object> updates);

    MedusaCartResponse selectPaymentSession(String cartId, String providerId);

    MedusaOrderResponse completeCart(String cartId);
}
