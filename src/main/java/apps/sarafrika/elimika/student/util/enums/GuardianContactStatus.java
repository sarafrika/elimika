package apps.sarafrika.elimika.student.util.enums;

import java.util.Locale;

/** Stored lifecycle of a named guardian; expiry is derived, see {@link StudentGuardianState}. */
public enum GuardianContactStatus {
    INVITED,
    LINKED,
    DECLINED,
    REMOVED;

    public static GuardianContactStatus fromValue(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
