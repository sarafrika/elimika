package apps.sarafrika.elimika.shared.utils.recurrence;

import java.time.LocalDate;

/**
 * Frequency-and-bounds description of a recurring session template.
 * <p>
 * Termination semantics (matching historic class scheduling behaviour):
 * when {@code occurrenceCount} is positive it wins and {@code endDate} is ignored;
 * otherwise {@code endDate} (inclusive) bounds the series; when neither is set
 * a single occurrence is produced.
 *
 * @param frequency       recurrence frequency; {@code null} means no recurrence (single occurrence)
 * @param intervalValue   gap between recurrences in frequency units; defaults to 1
 * @param daysOfWeek      comma separated day names, WEEKLY only (e.g. "MONDAY,WEDNESDAY")
 * @param dayOfMonth      day of month to repeat on, MONTHLY only (clamped to month length)
 * @param endDate         inclusive end date for the series
 * @param occurrenceCount number of occurrences to generate
 */
public record RecurrencePattern(RecurrenceFrequency frequency,
                                Integer intervalValue,
                                String daysOfWeek,
                                Integer dayOfMonth,
                                LocalDate endDate,
                                Integer occurrenceCount) {
}
