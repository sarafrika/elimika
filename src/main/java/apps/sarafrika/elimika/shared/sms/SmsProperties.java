package apps.sarafrika.elimika.shared.sms;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SMS delivery: {@code log} writes to the application log, {@code celcom} sends through Celcom Africa. */
@Getter
@Setter
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String provider = "log";
    private Celcom celcom = new Celcom();
    private Log log = new Log();

    @Getter
    @Setter
    public static class Celcom {
        private String baseUrl = "https://isms.celcomafrica.com";
        private String apiKey;
        private String partnerId;
        /** Registered sender ID. */
        private String shortcode;
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Getter
    @Setter
    public static class Log {
        /** Writes message text to the log; only for a local stack, where nothing reaches a phone. */
        private boolean revealContent = false;
    }
}
