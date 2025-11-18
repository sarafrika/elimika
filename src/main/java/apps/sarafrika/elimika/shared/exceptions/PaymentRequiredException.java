package apps.sarafrika.elimika.shared.exceptions;

/**
 * Thrown when an operation requires payment before proceeding (HTTP 402).
 */
public class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException(String message) {
        super(message);
    }
}
