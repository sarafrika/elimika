package apps.sarafrika.elimika.shared.sms;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Delivers nothing; logs that a message would have gone, with its text only when explicitly allowed. */
@Slf4j
@RequiredArgsConstructor
public class LoggingSmsSender implements SmsSender {

    private final boolean revealContent;

    @Override
    public SmsDelivery send(String msisdn, String message) {
        if (revealContent) {
            log.info("SMS (not sent, log provider) to {}: {}", Msisdn.mask(msisdn), message);
        } else {
            log.info("SMS (not sent, log provider) to {}; {} characters", Msisdn.mask(msisdn), message.length());
        }
        return SmsDelivery.accepted("log-" + UUID.randomUUID());
    }
}
