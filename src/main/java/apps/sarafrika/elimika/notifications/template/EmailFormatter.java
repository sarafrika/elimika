package apps.sarafrika.elimika.notifications.template;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Formats template values for readers; exposed to templates as {@code fmt}.
 * Stored times are UTC, so they are shown in the display zone with its abbreviation (e.g. EAT).
 */
public class EmailFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm a z", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);

    private final ZoneId zone;

    public EmailFormatter(ZoneId zone) {
        this.zone = zone;
    }

    public String dateTime(Object value) {
        ZonedDateTime zoned = toZoned(value);
        if (zoned != null) {
            return DATE_TIME.format(zoned);
        }
        LocalDate date = value instanceof LocalDate localDate ? localDate : null;
        return date != null ? DATE.format(date) : text(value);
    }

    public String date(Object value) {
        if (value instanceof LocalDate localDate) {
            return DATE.format(localDate);
        }
        ZonedDateTime zoned = toZoned(value);
        return zoned != null ? DATE.format(zoned) : text(value);
    }

    public String money(Object value) {
        if (value == null) {
            return "";
        }
        try {
            BigDecimal amount = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString().trim());
            return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(amount);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    public String capitalize(Object value) {
        String text = text(value).trim();
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** First word of a full name, for greetings. */
    public String firstName(Object value) {
        String text = text(value).trim();
        int space = text.indexOf(' ');
        return space > 0 ? text.substring(0, space) : text;
    }

    /** True when the value is present and not blank, for th:if on optional fields. */
    public boolean has(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof java.util.Collection<?> collection) {
            return !collection.isEmpty();
        }
        return !value.toString().isBlank();
    }

    private ZonedDateTime toZoned(Object value) {
        if (value instanceof ZonedDateTime zoned) {
            return zoned.withZoneSameInstant(zone);
        }
        if (value instanceof OffsetDateTime offset) {
            return offset.atZoneSameInstant(zone);
        }
        if (value instanceof LocalDateTime local) {
            return local.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
        }
        if (value instanceof Instant instant) {
            return instant.atZone(zone);
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(zone);
        }
        if (value instanceof CharSequence raw) {
            return parse(raw.toString().trim());
        }
        return null;
    }

    private ZonedDateTime parse(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {
            // try the next shape
        }
        try {
            return Instant.parse(text).atZone(zone);
        } catch (DateTimeParseException ignored) {
            // try the next shape
        }
        try {
            return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
