package apps.sarafrika.elimika.shared.spi.timetabling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only view of instructor time holds for modules that timetabling itself depends on.
 */
public interface InstructorTimeHoldLookupService {

    /**
     * TENTATIVE and FIRM holds overlapping the inclusive date range, named after their job and organisation.
     */
    List<InstructorTimeHoldEntry> findActiveHolds(UUID instructorUuid, LocalDate startDate, LocalDate endDate);

    /**
     * Whether a FIRM hold overlaps the UTC window. TENTATIVE holds never count.
     */
    boolean hasFirmHold(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
}
