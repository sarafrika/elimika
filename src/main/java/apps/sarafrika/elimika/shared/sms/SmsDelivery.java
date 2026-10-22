package apps.sarafrika.elimika.shared.sms;

/** What the provider said about one message. */
public record SmsDelivery(boolean accepted, String providerMessageId, String failure) {

    public static SmsDelivery accepted(String providerMessageId) {
        return new SmsDelivery(true, providerMessageId, null);
    }

    public static SmsDelivery refused(String failure) {
        return new SmsDelivery(false, null, failure);
    }
}
