package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared access checks for student-facing quiz flows (viewing, taking and submitting).
 * <p>
 * Centralises the ownership and visibility rules so the read path
 * ({@code StudentQuizViewService}) and the write path ({@code StudentQuizSubmissionService})
 * enforce identical guarantees.
 */
@Component
@RequiredArgsConstructor
public class StudentQuizAccessValidator {

    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with ID %s not found";

    private final LessonRepository lessonRepository;
    private final LearnerAssessmentScope learnerAssessmentScope;
    private final DomainSecurityService domainSecurityService;
    private final CourseSecuritySpi courseSecurityService;

    /**
     * Resolves the enrolment the caller should act through for this quiz, and asserts they may use
     * it: it must belong to the quiz's course, permit access, and — for students — be their own.
     * <p>
     * The enrolment UUID is optional. A learner does not need to name their own enrolment, and a
     * class-enrolment UUID sent where a course-enrolment UUID was expected is translated rather than
     * rejected. See {@link LearnerAssessmentScope#resolveEnrollment(UUID, UUID)}.
     *
     * @param enrollmentUuid enrolment from the request, may be {@code null}
     */
    public CourseEnrollment requireEnrollmentAccess(Quiz quiz, UUID enrollmentUuid) {
        return learnerAssessmentScope.resolveEnrollment(resolveCourseUuid(quiz), enrollmentUuid);
    }

    /**
     * Asserts the quiz is visible to students (published and active). Uses the not-found
     * template so unpublished quizzes are indistinguishable from missing ones.
     */
    public void requireStudentVisibleQuiz(Quiz quiz) {
        if (quiz.getStatus() != ContentStatus.PUBLISHED || !Boolean.TRUE.equals(quiz.getActive())) {
            throw new ResourceNotFoundException(String.format(QUIZ_NOT_FOUND_TEMPLATE, quiz.getUuid()));
        }
    }

    public UUID resolveCourseUuid(Quiz quiz) {
        Lesson lesson = lessonRepository.findByUuid(quiz.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Lesson with ID %s not found", quiz.getLessonUuid())));
        return lesson.getCourseUuid();
    }

    /**
     * Whether the caller marks this course, and so may open a submitted-but-ungraded attempt in
     * order to grade its text responses — which the learner who sat it may not do.
     * <p>
     * Asked per course rather than per role. Holding the instructor or course_creator domain says
     * only that the caller teaches <em>somewhere</em>, and an ungraded attempt shows the correct
     * answers alongside the learner's, so a platform-wide test would hand every instructor every
     * course's answer key the moment a learner submitted.
     */
    public boolean marksCourse(UUID courseUuid) {
        return courseSecurityService.canManageCourseGradebook(courseUuid)
                || domainSecurityService.isPlatformAdmin();
    }
}
