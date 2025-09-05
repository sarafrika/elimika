package apps.sarafrika.elimika.shared.exceptions;

public class SmtpAuthenticationException extends RuntimeException{
    public SmtpAuthenticationException(String message) {
        super(message);
    }

    public SmtpAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
