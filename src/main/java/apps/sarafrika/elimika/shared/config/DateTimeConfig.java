package apps.sarafrika.elimika.shared.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Pins the JVM's default time zone to UTC at startup.
 * <p>
 * Elimika stores every instant in UTC and maps most columns to zone-less
 * {@link java.time.LocalDateTime}. Hibernate's {@code timestamp <-> LocalDateTime}
 * conversion, {@code LocalDateTime.now()} used by JPA auditing, and any other
 * default-zone arithmetic all depend on the JVM default zone. Forcing it to UTC
 * (in addition to {@code TZ=UTC} in the container) guarantees consistent behavior
 * across local development, CI and production regardless of the host's zone.
 */
@Slf4j
@Configuration
public class DateTimeConfig {

    @PostConstruct
    public void enforceUtcDefaultTimeZone() {
        final TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("Default JVM time zone pinned to UTC (was {})", previous.getID());
    }
}
