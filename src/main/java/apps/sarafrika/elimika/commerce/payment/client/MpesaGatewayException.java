package apps.sarafrika.elimika.commerce.payment.client;

/**
 * Raised when a call to the mpesa-service gateway fails or returns an unusable response.
 */
public class MpesaGatewayException extends RuntimeException {

    public MpesaGatewayException(String message) {
        super(message);
    }

    public MpesaGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
