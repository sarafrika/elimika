package apps.sarafrika.elimika.resourcing.spi;

import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;

import java.util.List;
import java.util.UUID;

/**
 * Request to reserve one resource across a set of occurrence windows.
 *
 * @param resourceUuid the resource to reserve
 * @param quantity     units to reserve per window (must be 1 for venues)
 * @param windows      the concrete occurrence windows to reserve
 */
public record ResourceBookingRequest(UUID resourceUuid,
                                     int quantity,
                                     List<OccurrenceWindow> windows) {
}
