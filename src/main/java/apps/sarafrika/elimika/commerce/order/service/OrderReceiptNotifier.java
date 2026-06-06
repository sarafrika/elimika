package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

        Map<String, Object> variables = receiptVariables(order);
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(),
                resolvedUserId,
                email,
                recipientName,
                "ORDER_PAYMENT_RECEIPT",
                "NORMAL",
                "POPUP",
                "Payment received",
                receiptBody(variables),
                "/dashboard/transactions",
                variables,
                Set.of("email", "in_app"),
                "order-payment-receipt:" + variables.getOrDefault("orderId", order.getId()),
                LocalDateTime.now(ZoneOffset.UTC),
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

    private Map<String, Object> receiptVariables(OrderResponse order) {
        Map<String, Object> variables = new HashMap<>();
        putIfNotNull(variables, "orderId", order.getId());
        putIfNotNull(variables, "orderDisplayId", order.getDisplayId());
        putIfNotNull(variables, "paymentStatus", order.getPaymentStatus());
        putIfNotNull(variables, "currencyCode", order.getCurrencyCode());
        putIfNotNull(variables, "subtotal", order.getSubtotal());
        putIfNotNull(variables, "total", order.getTotal());
        putIfNotNull(variables, "createdAt", order.getCreatedAt());
        if (order.getPlatformFee() != null) {
            putIfNotNull(variables, "platformFeeAmount", order.getPlatformFee().amount());
            putIfNotNull(variables, "platformFeeCurrency", order.getPlatformFee().currency());
            putIfNotNull(variables, "platformFeeMode", order.getPlatformFee().mode());
            putIfNotNull(variables, "platformFeeRate", order.getPlatformFee().rate());
        }
        variables.put("items", order.getItems() == null ? List.of() : order.getItems());
        return variables;
    }

    private void putIfNotNull(Map<String, Object> variables, String key, Object value) {
        if (value != null) {
            variables.put(key, value);
        }
    }

    private String receiptBody(Map<String, Object> variables) {
        Object total = variables.getOrDefault("total", "your order");
        Object currency = variables.getOrDefault("currencyCode", "");
        return "Your payment for " + currency + " " + total + " was completed successfully.";
    }
}
