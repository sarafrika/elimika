package apps.sarafrika.elimika.payout.service;

/**
 * Raised when a delivered session could not be turned into an obligation.
 * <p>
 * Thrown deliberately rather than swallowed: it is what leaves the Modulith event publication
 * incomplete so the accrual is retried instead of being lost in a log line.
 */
public class InstructorObligationAccrualFailedException extends RuntimeException {

    public InstructorObligationAccrualFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
