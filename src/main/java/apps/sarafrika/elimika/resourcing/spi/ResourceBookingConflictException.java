package apps.sarafrika.elimika.resourcing.spi;

/**
 * Raised when requested resource bookings cannot be satisfied; carries the
 * per-occurrence conflict report for the API response.
 */
public class ResourceBookingConflictException extends RuntimeException {

    private final transient ResourceValidationReport report;

    public ResourceBookingConflictException(String message, ResourceValidationReport report) {
        super(message);
        this.report = report;
    }

    public ResourceValidationReport getReport() {
        return report;
    }
}
