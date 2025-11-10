package apps.sarafrika.elimika.shared.exceptions;

/**
 * Raised when a student attempts to enroll in a course or class that falls outside
 * the configured age limits.
 */
public class AgeRestrictionException extends RuntimeException {

    public AgeRestrictionException(String message) {
        super(message);
    }
}
