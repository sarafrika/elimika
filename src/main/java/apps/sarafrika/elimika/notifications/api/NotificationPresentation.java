package apps.sarafrika.elimika.notifications.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum NotificationPresentation {
    POPUP("POPUP"),
    INBOX("INBOX");

    private static final Map<String, NotificationPresentation> VALUE_MAP = new HashMap<>();

    static {
        for (NotificationPresentation presentation : NotificationPresentation.values()) {
            VALUE_MAP.put(presentation.value, presentation);
            VALUE_MAP.put(presentation.value.toLowerCase(Locale.ROOT), presentation);
        }
    }

    private final String value;

    NotificationPresentation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationPresentation fromValue(String value) {
        if (value == null) {
            return null;
        }
        NotificationPresentation presentation = VALUE_MAP.get(value.trim());
        if (presentation == null) {
            presentation = VALUE_MAP.get(value.trim().toLowerCase(Locale.ROOT));
        }
        if (presentation == null) {
            throw new IllegalArgumentException("Unknown NotificationPresentation: " + value);
        }
        return presentation;
    }

    public static NotificationPresentation fromDatabaseValue(String value) {
        return fromValue(value);
    }

    public String getDatabaseValue() {
        return value;
    }
}
