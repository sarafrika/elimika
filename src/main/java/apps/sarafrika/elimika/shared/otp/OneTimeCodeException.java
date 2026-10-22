package apps.sarafrika.elimika.shared.otp;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A code could not be issued or did not verify; the message is safe to show the user. */
public class OneTimeCodeException extends ResponseStatusException {

    public OneTimeCodeException(HttpStatus status, String reason) {
        super(status, reason);
    }

    static OneTimeCodeException wrong(int attemptsLeft) {
        return new OneTimeCodeException(HttpStatus.BAD_REQUEST, attemptsLeft > 0
                ? "That code is not right. " + attemptsLeft + (attemptsLeft == 1 ? " try" : " tries") + " left."
                : "That code is not right and can no longer be used. Request a new one.");
    }

    static OneTimeCodeException expired() {
        return new OneTimeCodeException(HttpStatus.BAD_REQUEST, "This code has expired or was replaced. Request a new one.");
    }

    static OneTimeCodeException tooSoon(long seconds) {
        return new OneTimeCodeException(HttpStatus.TOO_MANY_REQUESTS,
                "Please wait " + seconds + " seconds before asking for another code.");
    }

    static OneTimeCodeException tooMany() {
        return new OneTimeCodeException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many codes were requested. Try again in an hour.");
    }

    static OneTimeCodeException notSent() {
        return new OneTimeCodeException(HttpStatus.BAD_GATEWAY,
                "We could not send the SMS. Check the number and try again.");
    }
}
