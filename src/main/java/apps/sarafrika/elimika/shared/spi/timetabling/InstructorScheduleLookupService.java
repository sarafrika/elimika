package apps.sarafrika.elimika.shared.spi.timetabling;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SPI for retrieving instructor schedule projections without exposing timetabling internals.
 */
public interface InstructorScheduleLookupService {

    /**
     * Retrieves scheduled instances for an instructor within a date range.
     *
     * @param instructorUuid instructor identifier
     * @param startDate inclusive start date
     * @param endDate inclusive end date
     * @return schedule entries ordered as provided by the timetabling module
     */
    List<InstructorScheduleEntry> getScheduleForInstructor(UUID instructorUuid, LocalDate startDate, LocalDate endDate);
}
