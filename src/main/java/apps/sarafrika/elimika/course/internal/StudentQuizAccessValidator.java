package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Course enrollment with ID %s not found";

    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final DomainSecurityService domainSecurityService;

    /**
     * Loads the enrollment and asserts the current caller may use it for the given quiz:
     * the enrollment must belong to the quiz's course, permit access, and — for students —
     * belong to the authenticated student.
     */
    public CourseEnrollment requireEnrollmentAccess(Quiz quiz, UUID enrollmentUuid) {
        CourseEnrollment enrollment = courseEnrollmentRepository.findByUuid(enrollmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid)));

        UUID courseUuid = resolveCourseUuid(quiz);
        if (!courseUuid.equals(enrollment.getCourseUuid())) {
            throw new AccessDeniedException("Course enrollment does not belong to this quiz.");
        }
        if (enrollment.getStatus() == null || !enrollment.getStatus().allowsAccess()) {
            throw new AccessDeniedException("Course enrollment does not allow quiz access.");
        }
        if (domainSecurityService.isStudent()
                && !domainSecurityService.isStudentWithUuid(enrollment.getStudentUuid())) {
            throw new AccessDeniedException("Students may only access quizzes for their own course enrollment.");
        }
        return enrollment;
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
}
