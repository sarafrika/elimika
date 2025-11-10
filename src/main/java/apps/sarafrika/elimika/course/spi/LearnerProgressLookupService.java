package apps.sarafrika.elimika.course.spi;

import java.util.List;
import java.util.UUID;

/**
 * SPI that exposes read-only learner progress data for other modules (e.g., student guardians).
 */
public interface LearnerProgressLookupService {

    /**
     * Returns the most recent course enrollments for a learner.
     *
     * @param studentUuid learner identifier
     * @param limit       max number of enrollments to return
     * @return ordered list (newest first) of course progress snapshots
     */
    List<LearnerCourseProgressView> findRecentCourseProgress(UUID studentUuid, int limit);

    /**
     * Returns the most recent training program enrollments for a learner.
     *
     * @param studentUuid learner identifier
     * @param limit       max number of enrollments to return
     * @return ordered list (newest first) of program progress snapshots
     */
    List<LearnerProgramProgressView> findRecentProgramProgress(UUID studentUuid, int limit);
}
