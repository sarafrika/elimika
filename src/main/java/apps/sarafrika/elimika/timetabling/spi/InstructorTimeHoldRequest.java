package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.utils.recurrence.OccurrenceWindow;

import java.util.List;
import java.util.UUID;

/**
 * Request to hold every occurrence window of one marketplace job application. Holds are replaced
 * per application, the title is cached so a calendar needs no second lookup, and a null timezone
 * defaults to UTC.
 */
public record InstructorTimeHoldRequest(UUID instructorUuid,
                                        UUID instructorUserUuid,
                                        UUID organisationUuid,
                                        UUID jobUuid,
                                        UUID applicationUuid,
                                        String title,
                                        String timezone,
                                        List<OccurrenceWindow> windows) {
}
