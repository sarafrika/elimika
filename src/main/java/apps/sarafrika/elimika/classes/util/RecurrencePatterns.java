package apps.sarafrika.elimika.classes.util;

import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrenceFrequency;
import apps.sarafrika.elimika.shared.utils.recurrence.RecurrencePattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Maps the classes module recurrence representations onto the shared
 * {@link RecurrencePattern} consumed by {@code RecurrenceExpander}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecurrencePatterns {

    public static RecurrencePattern fromRecurrenceDTO(ClassRecurrenceDTO recurrence) {
        if (recurrence == null || recurrence.recurrenceType() == null) {
            return null;
        }
        return new RecurrencePattern(
                RecurrenceFrequency.valueOf(recurrence.recurrenceType().name()),
                recurrence.intervalValue(),
                recurrence.daysOfWeek(),
                recurrence.dayOfMonth(),
                recurrence.endDate(),
                recurrence.occurrenceCount());
    }
}
