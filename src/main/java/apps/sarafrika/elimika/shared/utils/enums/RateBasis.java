package apps.sarafrika.elimika.shared.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * The unit a class is priced in, fixed by the job that contracted it.
 * <p>
 * One basis governs both sides of the money: the learner is charged and the instructor is paid on
 * the same multiplier, so the margin between the two prices stays a like-for-like subtraction. A
 * class priced per hour and paid per session would make every margin figure a guess.
 */
public enum RateBasis {

    /** Billed on total scheduled time. The historical behaviour and the default. */
    PER_HOUR("per_hour"),

    /** Billed once per scheduled session, whatever its length. */
    PER_SESSION("per_session"),

    /** Billed once per calendar day that holds any session, so two sessions in a day bill once. */
    PER_DAY("per_day");

    private final String value;
    private static final Map<String, RateBasis> VALUE_MAP = new HashMap<>();

    static {
        for (RateBasis basis : values()) {
            VALUE_MAP.put(basis.value, basis);
            VALUE_MAP.put(basis.value.toUpperCase(), basis);
            VALUE_MAP.put(basis.name(), basis);
        }
    }

    RateBasis(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RateBasis fromValue(String value) {
        if (value == null) {
            return PER_HOUR;
        }
        RateBasis basis = VALUE_MAP.get(value);
        if (basis == null) {
            throw new IllegalArgumentException("Unknown RateBasis: " + value);
        }
        return basis;
    }
}
