package apps.sarafrika.elimika.common.exceptions;

public class InvalidCsvFormatException extends RuntimeException {
    public InvalidCsvFormatException(String message) {
        super(message);
    }

    public InvalidCsvFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
