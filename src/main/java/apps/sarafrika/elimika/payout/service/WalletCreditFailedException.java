package apps.sarafrika.elimika.payout.service;

/**
 * Raised when one or more wallet credits for a captured order could not be applied.
 * <p>
 * Throwing this out of {@link OrderCaptureWalletCreditListener} is deliberate: it is the signal that
 * leaves the Spring Modulith event publication incomplete, which is what makes the money owed
 * recoverable. Swallowing it would lose the credit permanently.
 * <p>
 * It never reaches the checkout caller - the listener is asynchronous and runs after the order has
 * been recorded, so the order still completes.
 */
public class WalletCreditFailedException extends RuntimeException {

    public WalletCreditFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
