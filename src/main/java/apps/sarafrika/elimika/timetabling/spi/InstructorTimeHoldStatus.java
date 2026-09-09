package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lifecycle state of an instructor time hold. Only FIRM counts as a clash, since an instructor may
 * apply to overlapping jobs. CONFIRMED and RELEASED are both terminal, split so an audit can tell
 * "this became a class" from "this went nowhere".
 */
public enum InstructorTimeHoldStatus {
    TENTATIVE("TENTATIVE", "Applied for the job, decision pending; pencilled in but never a clash"),
    FIRM("FIRM", "Hired for the job, class not created yet; blocks other scheduling"),
    CONFIRMED("CONFIRMED", "Superseded by a real scheduled instance"),
    RELEASED("RELEASED", "Released (rejected, withdrawn, cancelled, expired or never scheduled)");

    private final String value;
    private final String description;
    private static final Map<String, InstructorTimeHoldStatus> VALUE_MAP = new HashMap<>();

    static {
        for (InstructorTimeHoldStatus status : InstructorTimeHoldStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }

    InstructorTimeHoldStatus(String value, String description) {
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
    public static InstructorTimeHoldStatus fromValue(String value) {
        InstructorTimeHoldStatus status = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (status == null) {
            throw new IllegalArgumentException("Unknown InstructorTimeHoldStatus: " + value);
        }
        return status;
    }
}
