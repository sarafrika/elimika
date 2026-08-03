package apps.sarafrika.elimika.payout.enums;

import java.util.Locale;

/**
 * Lifecycle of a single session's pay owed by an organisation to an instructor.
 * <p>
 * There is no {@code PAID} distinct from {@code SETTLED}: the platform never moves this money, so
 * the only thing it can assert is that the organisation told us it paid, and against what reference.
 */
public enum InstructorObligationStatus {

    /** Session delivered, money owed, nothing recorded against it yet. */
    ACCRUED,

    /** The organisation has paid the instructor off-platform and recorded evidence of it. */
    SETTLED,

    /**
     * The obligation should never have existed — a session voided after the fact, a duplicate
     * accrual. Excluded from what is owed, but kept, because deleting a money row hides a mistake
     * instead of recording it.
     */
    CANCELLED,

    /** Contested by either side. Excluded from what is owed until it is resolved. */
    DISPUTED;

    public static InstructorObligationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return InstructorObligationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
