package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseTrainerDirectoryDTO;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * The read side of a course's delivery list: who may teach it, where, and — for the creator alone —
 * on what terms.
 */
public interface CourseTrainerDirectoryService {

    /**
     * Lists the trainers approved to deliver a course.
     * <p>
     * The caller's entitlement decides which query runs, not which fields are blanked afterwards: a
     * course creator or platform admin gets the projection carrying rate cards and the pending
     * count, everybody else gets one that never selects a rate column.
     *
     * @param courseUuid the course whose delivery list is wanted
     * @param pageable   paging and ordering; only {@code display_name}, {@code approved_at} and
     *                   {@code active_class_count} may be sorted on, and any other sort property is
     *                   rejected rather than ignored
     * @return the directory, with {@code rate_card} and {@code pending_count} present only for the
     *         course owner and platform administrators
     */
    CourseTrainerDirectoryDTO getTrainerDirectory(UUID courseUuid, Pageable pageable);
}
