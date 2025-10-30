package apps.sarafrika.elimika.course.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides read-only access to course assessment metadata for other modules.
 */
public interface CourseAssessmentLookupService {

    Optional<CourseAssignmentSummary> getAssignmentSummary(UUID assignmentUuid);

    Optional<CourseQuizSummary> getQuizSummary(UUID quizUuid);

    record CourseAssignmentSummary(UUID assignmentUuid, UUID lessonUuid, String title) {
    }

    record CourseQuizSummary(UUID quizUuid, UUID lessonUuid, String title) {
    }
}
