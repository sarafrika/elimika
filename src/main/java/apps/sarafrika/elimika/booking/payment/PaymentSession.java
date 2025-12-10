package apps.sarafrika.elimika.booking.payment;

public record PaymentSession(
        String sessionId,
        String paymentUrl,
        String engine
) { }
