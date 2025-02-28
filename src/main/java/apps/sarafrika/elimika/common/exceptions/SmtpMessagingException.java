package apps.sarafrika.elimika.common.exceptions;

public class SmtpMessagingException extends RuntimeException{
    public SmtpMessagingException(String message) {
        super(message);
    }

    public SmtpMessagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
