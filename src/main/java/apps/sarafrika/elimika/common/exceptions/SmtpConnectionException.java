package apps.sarafrika.elimika.common.exceptions;

public class SmtpConnectionException extends RuntimeException{
    public SmtpConnectionException(String message) {
        super(message);
    }

    public SmtpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
