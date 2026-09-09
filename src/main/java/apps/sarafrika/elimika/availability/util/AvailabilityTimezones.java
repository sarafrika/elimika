package apps.sarafrika.elimika.availability.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

// A slot stores a bare wall clock, which is only a moment in time once its zone is applied. Every
// boundary that publishes or evaluates a slot makes that conversion, so it lives in one place.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AvailabilityTimezones {

    private static final String DEFAULT_TIMEZONE = "UTC";

    // Slots written before the zone was captured were read as UTC everywhere, so an absent zone
    // stays UTC and those rows keep deciding exactly what they decided before.
    public static String normalize(String timezone) {
        String value = timezone == null || timezone.isBlank()
                ? DEFAULT_TIMEZONE
                : timezone.trim();
        try {
            ZoneId.of(value);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid IANA timezone: " + value, ex);
        }
        return value;
    }

    public static ZoneId resolve(String timezone) {
        return ZoneId.of(normalize(timezone));
    }

    public static LocalDateTime toUtc(LocalDateTime localDateTime, ZoneId zone) {
        return localDateTime.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime fromUtc(LocalDateTime utcDateTime, ZoneId zone) {
        return utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime();
    }
}
