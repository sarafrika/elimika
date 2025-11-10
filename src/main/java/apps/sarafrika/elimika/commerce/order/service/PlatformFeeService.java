package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;

import java.util.Optional;

public interface PlatformFeeService {

    Optional<PlatformFeeBreakdown> calculateFee(MedusaOrderResponse medusaOrder, CheckoutRequest checkoutContext);
}
