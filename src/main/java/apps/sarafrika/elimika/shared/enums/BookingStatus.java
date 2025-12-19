package apps.sarafrika.elimika.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum BookingStatus {
    PAYMENT_REQUIRED("payment_required", "Awaiting payment to confirm booking"),
    CONFIRMED("confirmed", "Booking confirmed and slot held"),
    CANCELLED("cancelled", "Booking cancelled by student or system"),
    PAYMENT_FAILED("payment_failed", "Payment attempt failed"),
    EXPIRED("expired", "Hold expired before payment confirmation"),
    ACCEPTED("accepted", "Instructor accepted the booking"),
    DECLINED("declined", "Instructor declined the booking");

    private static final Map<String, BookingStatus> VALUE_MAP = new HashMap<>();
    private final String value;
    private final String description;

    static {
        for (BookingStatus status : BookingStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(Locale.ROOT), status);
        }
    }

    BookingStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static BookingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        BookingStatus status = VALUE_MAP.get(value);
        if (status != null) {
            return status;
        }
        BookingStatus normalized = VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (normalized != null) {
            return normalized;
        }
        throw new IllegalArgumentException("Unknown BookingStatus: " + value);
    }
}
