package apps.sarafrika.elimika.shared.utils.recurrence;

import java.time.LocalDateTime;

/**
 * A single concrete occurrence expanded from a recurring session template.
 */
public record OccurrenceWindow(LocalDateTime start, LocalDateTime end) {
}
