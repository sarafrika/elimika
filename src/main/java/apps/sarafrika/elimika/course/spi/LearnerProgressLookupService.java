package apps.sarafrika.elimika.course.spi;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * SPI that exposes read-only learner progress data for other modules (e.g., student guardians).
 */
public interface LearnerProgressLookupService {

    /**
     * Returns all course enrollments for a learner ordered from most recent to oldest.
     *
     * @param studentUuid learner identifier
     * @param pageable    pagination information
     * @return ordered page of course progress snapshots
     */
    Page<LearnerCourseProgressView> findCourseProgress(UUID studentUuid, Pageable pageable);

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
