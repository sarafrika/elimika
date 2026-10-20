package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Where a session report is: being captured, submitted (still editable in the grace window), or locked. */
public enum SessionReportStatus {
    DRAFT("draft"),
    SUBMITTED("submitted"),
    LOCKED("locked");

    private final String value;

    SessionReportStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SessionReportStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /** Parses query values such as draft or SUBMITTED; null or empty means every status. */
    public static List<SessionReportStatus> parseAll(Collection<String> values) {
        return values == null ? List.of() : values.stream().map(SessionReportStatus::fromValue).toList();
    }
}
