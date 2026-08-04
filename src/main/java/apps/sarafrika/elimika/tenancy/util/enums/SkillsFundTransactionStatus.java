package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Lifecycle of a single movement within an organisation's skills fund.
 * <p>
 * This replaces a free-form {@code VARCHAR} that was compared case-insensitively against a hard-coded
 * list of words. Anything outside that list was accepted on write and then silently excluded from
 * every aggregate — the money stayed in the table but disappeared from the totals. A closed set,
 * mirrored by a CHECK constraint in the database, makes that failure impossible rather than merely
 * unlikely.
 * <p>
 * There is no {@code COMPLETED} distinct from {@link #DISBURSED}. The legacy schema carried both
 * words for the same fact — money left the fund — with only {@code Completed} reachable from the
 * dashboard, and counted them differently in the summary. They are one state.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-08-04
 */
public enum SkillsFundTransactionStatus {

    /** Requested, nothing committed. Counts towards {@code pending} only. */
    PENDING,

    /** Earmarked against the fund but not yet signed off. */
    ALLOCATED,

    /** Signed off and awaiting payment. */
    APPROVED,

    /**
     * The money has left the fund. This is the terminal state, and the only one that reduces what
     * the fund has remaining.
     */
    DISBURSED;

    /**
     * True when the movement represents money committed against the fund rather than a bare request.
     * <p>
     * Money already disbursed was necessarily committed first, so {@code allocated} is cumulative and
     * includes {@link #DISBURSED}. That is the behaviour the previous implementation had for
     * {@code Completed} — counted into both {@code allocated} and {@code disbursed} — now applied
     * uniformly instead of depending on which of two synonyms the caller happened to type.
     */
    public boolean isCommitted() {
        return this == ALLOCATED || this == APPROVED || this == DISBURSED;
    }

    /** True when the money has actually left the fund. */
    public boolean isDisbursed() {
        return this == DISBURSED;
    }

    /**
     * Parses a stored or submitted value, tolerating case and surrounding whitespace, and folding the
     * legacy synonym {@code Completed} onto {@link #DISBURSED} so data written before the enum
     * existed keeps working.
     *
     * @throws IllegalArgumentException when the value names no state — never silently defaulted,
     *                                  because a guessed status moves money between totals
     */
    @JsonCreator
    public static SkillsFundTransactionStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETED".equals(normalised)) {
            return DISBURSED;
        }
        try {
            return valueOf(normalised);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown SkillsFundTransactionStatus: " + value);
        }
    }

    @JsonValue
    public String getValue() {
        return name();
    }
}
