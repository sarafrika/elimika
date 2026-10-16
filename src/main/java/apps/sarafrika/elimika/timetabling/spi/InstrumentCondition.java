package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** The state of the instrument a learner played in a session, as instructors note it on the register. */
public enum InstrumentCondition {
    GOOD("GOOD"),
    NEEDS_REPAIR("NEEDS_REPAIR"),
    NO_EQUIPMENT("NO_EQUIPMENT");

    private final String value;

    InstrumentCondition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InstrumentCondition fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (InstrumentCondition condition : values()) {
            if (condition.value.equals(normalised)) {
                return condition;
            }
        }
        throw new IllegalArgumentException("Unknown InstrumentCondition: " + value);
    }
}
