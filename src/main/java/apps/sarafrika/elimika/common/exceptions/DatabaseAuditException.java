package apps.sarafrika.elimika.common.exceptions;

public class DatabaseAuditException extends RuntimeException {
    public DatabaseAuditException(String message) {
        super(message);
    }

    public DatabaseAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
