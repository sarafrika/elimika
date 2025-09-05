package apps.sarafrika.elimika.shared.exceptions;

public class SmtpConnectionException extends RuntimeException{
    public SmtpConnectionException(String message) {
        super(message);
    }

    public SmtpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
