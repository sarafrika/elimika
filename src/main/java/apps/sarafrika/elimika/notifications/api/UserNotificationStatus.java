package apps.sarafrika.elimika.notifications.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum UserNotificationStatus {
    UNREAD("UNREAD"),
    READ("READ"),
    ARCHIVED("ARCHIVED");

    private static final Map<String, UserNotificationStatus> VALUE_MAP = new HashMap<>();

    static {
        for (UserNotificationStatus status : UserNotificationStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(Locale.ROOT), status);
        }
    }

    private final String value;

    UserNotificationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserNotificationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        UserNotificationStatus status = VALUE_MAP.get(value.trim());
        if (status == null) {
            status = VALUE_MAP.get(value.trim().toLowerCase(Locale.ROOT));
        }
        if (status == null) {
            throw new IllegalArgumentException("Unknown UserNotificationStatus: " + value);
        }
        return status;
    }

    public static UserNotificationStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }

    public String getDatabaseValue() {
        return value;
    }
}
