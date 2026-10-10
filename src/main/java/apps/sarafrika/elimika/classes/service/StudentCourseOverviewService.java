package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Composes a learner's enrolled classes with course, instructor, next session, progress and
 * outstanding assessments in a fixed number of queries, however many classes are on the page.
 */
public interface StudentCourseOverviewService {

    Page<StudentCourseOverviewItemDTO> getCourseOverview(UUID studentUuid, Pageable pageable);
}
