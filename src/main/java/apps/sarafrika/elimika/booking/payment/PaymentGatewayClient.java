package apps.sarafrika.elimika.booking.payment;

import apps.sarafrika.elimika.booking.model.Booking;

public interface PaymentGatewayClient {

    PaymentSession initiatePayment(Booking booking);

    default void cancelPayment(String sessionId) {
        // no-op default to keep implementations optional
    }
}
