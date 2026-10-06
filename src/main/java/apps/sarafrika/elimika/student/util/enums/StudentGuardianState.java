package apps.sarafrika.elimika.student.util.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** What the API reports for a student's guardian: their access, or where their invitation stands. */
public enum StudentGuardianState {
    LINKED,
    INVITED,
    EXPIRED,
    DECLINED,
    REVOKED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
