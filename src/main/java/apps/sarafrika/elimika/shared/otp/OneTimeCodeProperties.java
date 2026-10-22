package apps.sarafrika.elimika.shared.otp;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** One-time codes sent by SMS. */
@Getter
@Setter
@ConfigurationProperties(prefix = "otp")
public class OneTimeCodeProperties {

    /** Key for hashing codes at rest; blank means a random key per start, so codes die with a restart. */
    private String hmacSecret;
    private Duration ttl = Duration.ofMinutes(10);
    private int maxAttempts = 5;
    private Duration resendCooldown = Duration.ofSeconds(60);
    private int maxPerHour = 5;
}
