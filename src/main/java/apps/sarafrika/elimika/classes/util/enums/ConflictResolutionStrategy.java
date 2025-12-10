package apps.sarafrika.elimika.classes.util.enums;

/**
 * Conflict handling strategy for inline class session templates.
 * FAIL: abort scheduling on the first conflict and return 409 with conflicts.
 * SKIP: keep scheduling non-conflicting occurrences, return conflicts for the skipped dates.
 * ROLLOVER: push conflicting occurrences forward by the recurrence interval (bounded retries) and extend the series; return any unrecoverable conflicts.
 */
public enum ConflictResolutionStrategy {
    FAIL,
    SKIP,
    ROLLOVER
}
