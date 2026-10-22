package apps.sarafrika.elimika.shared.sms;

/** Sends one transactional SMS. Implementations never log the message of a real send. */
public interface SmsSender {

    /** {@code msisdn} is already normalised (2547XXXXXXXX). Never throws for a provider refusal. */
    SmsDelivery send(String msisdn, String message);
}
