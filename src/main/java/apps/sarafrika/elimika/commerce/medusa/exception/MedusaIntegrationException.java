package apps.sarafrika.elimika.commerce.medusa.exception;

/**
 * Unchecked exception thrown when the Medusa API returns an error or an unexpected payload.
 */
public class MedusaIntegrationException extends RuntimeException {

    public MedusaIntegrationException(String message) {
        super(message);
    }

    public MedusaIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
