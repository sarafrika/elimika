package apps.sarafrika.elimika.student.spi;

/**
 * Exception raised when a student fails the age-gating decision for a feature.
 */
public class StudentAgeGateException extends RuntimeException {
    public StudentAgeGateException(String message) {
        super(message);
    }
}
