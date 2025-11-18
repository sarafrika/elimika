package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.notifications.api.events.OrderPaymentReceiptEvent;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes receipt notifications when an order completes.
 */
@Component
@RequiredArgsConstructor
public class OrderReceiptNotifier {

    private final ApplicationEventPublisher eventPublisher;
    private final UserLookupService userLookupService;

    public void sendReceipt(OrderResponse order, CheckoutRequest checkoutRequest) {
        if (order == null || checkoutRequest == null) {
            return;
        }

        String email = checkoutRequest.getCustomerEmail();
        if (!StringUtils.hasText(email)) {
            return;
        }

        UUID resolvedUserId = userLookupService.findUserUuidByEmail(email).orElseGet(() ->
                UUID.nameUUIDFromBytes(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))
        );

        String recipientName = userLookupService.getUserFullName(resolvedUserId)
                .orElse(email);

        UUID organizationId = findOrganization(resolvedUserId);

        OrderPaymentReceiptEvent event = OrderPaymentReceiptEvent.from(
                order,
                resolvedUserId,
                email,
                recipientName,
                organizationId
        );

        eventPublisher.publishEvent(event);
    }

    private UUID findOrganization(UUID userId) {
        List<UUID> organizations = userLookupService.getUserOrganizations(userId);
        if (organizations == null || organizations.isEmpty()) {
            return null;
        }
        return organizations.getFirst();
    }
}
