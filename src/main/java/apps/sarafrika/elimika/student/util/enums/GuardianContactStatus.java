package apps.sarafrika.elimika.student.util.enums;

import java.util.Locale;

/**
 * Stored lifecycle of a guardian a student named. Expiry is derived from the invitation date,
 * so it is not stored; see {@link StudentGuardianState} for what the API reports.
 */
public enum GuardianContactStatus {
    INVITED,
    LINKED,
    DECLINED,
    REMOVED;

    public static GuardianContactStatus fromValue(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
