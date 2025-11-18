package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Event published when an order is completed so the purchaser receives a receipt via email.
 */
public record OrderPaymentReceiptEvent(
        UUID notificationId,
        UUID recipientId,
        String recipientEmail,
        String recipientName,
        LocalDateTime createdAt,
        UUID organizationId,
        Map<String, Object> templateVariables
) implements NotificationEvent {

    public OrderPaymentReceiptEvent {
        Objects.requireNonNull(recipientId, "recipientId is required");
        Objects.requireNonNull(recipientEmail, "recipientEmail is required");
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        templateVariables = templateVariables == null ? Map.of() : Map.copyOf(templateVariables);
    }

    public static OrderPaymentReceiptEvent from(
            OrderResponse order,
            UUID recipientId,
            String recipientEmail,
            String recipientName,
            UUID organizationId
    ) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", order.getId());
        vars.put("orderDisplayId", order.getDisplayId());
        vars.put("paymentStatus", order.getPaymentStatus());
        vars.put("currencyCode", order.getCurrencyCode());
        vars.put("subtotal", order.getSubtotal());
        vars.put("total", order.getTotal());
        vars.put("createdAt", order.getCreatedAt());
        if (order.getPlatformFee() != null) {
            vars.put("platformFeeAmount", order.getPlatformFee().amount());
            vars.put("platformFeeCurrency", order.getPlatformFee().currency());
            vars.put("platformFeeMode", order.getPlatformFee().mode());
            vars.put("platformFeeRate", order.getPlatformFee().rate());
        }
        vars.put("items", order.getItems());

        return new OrderPaymentReceiptEvent(
                UUID.randomUUID(),
                recipientId,
                recipientEmail,
                recipientName,
                LocalDateTime.now(),
                organizationId,
                vars
        );
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.ORDER_PAYMENT_RECEIPT;
    }

    @Override
    public NotificationPriority getPriority() {
        return NotificationPriority.NORMAL;
    }

    @Override
    public UUID getNotificationId() {
        return notificationId;
    }

    @Override
    public UUID getRecipientId() {
        return recipientId;
    }

    @Override
    public String getRecipientEmail() {
        return recipientEmail;
    }

    @Override
    public String getRecipientName() {
        return recipientName;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public UUID getOrganizationId() {
        return organizationId;
    }

    @Override
    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }
}
