package apps.sarafrika.elimika.booking.payment;

import apps.sarafrika.elimika.booking.model.Booking;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NoOpPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentSession initiatePayment(Booking booking) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl = "https://payments.local/session/" + sessionId;
        String engine = booking.getPaymentEngine() != null ? booking.getPaymentEngine() : "placeholder";
        return new PaymentSession(sessionId, paymentUrl, engine);
    }
}
