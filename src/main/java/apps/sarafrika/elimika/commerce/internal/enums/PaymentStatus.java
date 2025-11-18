package apps.sarafrika.elimika.commerce.internal.enums;

/**
 * Payment progression for an order or payment attempt.
 */
public enum PaymentStatus {
    AWAITING_PAYMENT,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_CAPTURED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELED,
    FAILED
}
