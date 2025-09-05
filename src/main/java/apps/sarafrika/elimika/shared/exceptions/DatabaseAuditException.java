package apps.sarafrika.elimika.shared.exceptions;

public class DatabaseAuditException extends RuntimeException {
    public DatabaseAuditException(String message) {
        super(message);
    }

    public DatabaseAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
