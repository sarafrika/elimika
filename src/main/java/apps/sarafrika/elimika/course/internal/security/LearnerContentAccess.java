package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRubricAssociationRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Answers "may this caller read this piece of course material?" for material identified by its own
 * UUID rather than by course.
 * <p>
 * Quizzes, assignments and lessons carry no {@code courseUuid}, so an endpoint like
 * {@code GET /quizzes/{uuid}} cannot ask {@link CourseSecuritySpi#canReadCourseAsLearner(UUID)}
 * directly — it has to walk to the owning course first. This component owns that walk, so there is
 * exactly one quiz-to-course and assignment-to-course path in the codebase.
 * <p>
 * For learners the answer combines two things that must always travel together: enrolment in the
 * owning course, <em>and</em> the material being published. Teaching staff bypass the publish check
 * because drafting is their job.
 * <p>
 * Cost matters here: this runs inside {@code @PreAuthorize}, before the handler, once per item on a
 * page that lists many. Each walk is a single joined query
 * ({@code findCourseViewByUuid}) and its result is memoised for the request, so listing twenty
 * assignments from one course costs one material query each and a single enrolment lookup overall
 * rather than three per item.
 * <p>
 * Registered as {@code learnerContentAccess} for use from {@code @PreAuthorize}.
 */
@Component("learnerContentAccess")
@RequiredArgsConstructor
@Slf4j
public class LearnerContentAccess {

    private static final String CACHE_STAFF = "learnerContent.staff";
    private static final String CACHE_QUIZ = "learnerContent.quiz.";
    private static final String CACHE_ASSIGNMENT = "learnerContent.assignment.";
    private static final String CACHE_LESSON = "learnerContent.lesson.";
    private static final String CACHE_RUBRIC = "learnerContent.rubric.";

    private final QuizRepository quizRepository;
    private final AssignmentRepository assignmentRepository;
    private final LessonRepository lessonRepository;
    private final CourseRubricAssociationRepository courseRubricAssociationRepository;
    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * Whether the caller is teaching/administering staff rather than a learner. Staff see drafts;
     * learners never do.
     */
    public boolean isStaff() {
        return requestScopedCache.get(CACHE_STAFF, () ->
                domainSecurityService.isInstructorOrAdmin() || domainSecurityService.isCourseCreator());
    }

    /**
     * A quiz is readable by staff, or by a learner enrolled in its course once it is published and
     * active. Quiz metadata only — questions and answer keys stay behind the student-view flow.
     */
    public boolean canReadQuiz(UUID quizUuid) {
        return canRead(CACHE_QUIZ, quizUuid, quizRepository::findCourseViewByUuid);
    }

    /**
     * An assignment is readable by staff, or by a learner enrolled in its course once it is
     * published.
     */
    public boolean canReadAssignment(UUID assignmentUuid) {
        return canRead(CACHE_ASSIGNMENT, assignmentUuid, assignmentRepository::findCourseViewByUuid);
    }

    /**
     * A lesson is readable by staff, or by a learner enrolled in its course once it is published and
     * active.
     */
    public boolean canReadLesson(UUID lessonUuid) {
        return canRead(CACHE_LESSON, lessonUuid, lessonRepository::findCourseViewByUuid);
    }

    /**
     * A rubric is readable by staff, or by a learner enrolled in any course the rubric is associated
     * with. Learners are graded against rubrics, so they need to see the criteria they are marked
     * on — but only for courses they are actually in.
     * <p>
     * Resolved by intersecting the rubric's courses with the caller's enrolled courses: one query
     * for each side and a set membership test, rather than an enrolment lookup per association.
     */
    public boolean canReadRubric(UUID rubricUuid) {
        if (rubricUuid == null) {
            return false;
        }
        if (isStaff()) {
            return true;
        }
        return requestScopedCache.get(CACHE_RUBRIC + rubricUuid, () -> {
            try {
                Set<UUID> enrolledCourses = courseSecurityService.enrolledCourseUuids();
                if (enrolledCourses.isEmpty()) {
                    return false;
                }
                return courseRubricAssociationRepository.findByRubricUuid(rubricUuid).stream()
                        .map(CourseRubricAssociation::getCourseUuid)
                        .anyMatch(enrolledCourses::contains);
            } catch (Exception e) {
                log.error("Error checking learner rubric access for rubric: {}", rubricUuid, e);
                return false;
            }
        });
    }

    /**
     * Shared shape of the decision: staff pass on existence alone; a learner needs both an enrolment
     * in the owning course and published material. Anything that fails to resolve is a denial, never
     * a grant.
     */
    private boolean canRead(String cachePrefix, UUID uuid, Function<UUID, Optional<MaterialCourseView>> resolver) {
        if (uuid == null) {
            return false;
        }
        if (isStaff()) {
            return true;
        }
        return requestScopedCache.get(cachePrefix + uuid, () -> {
            try {
                return resolver.apply(uuid)
                        .map(material -> material.visibleToLearners()
                                && courseSecurityService.isEnrolledLearner(material.courseUuid()))
                        .orElse(false);
            } catch (Exception e) {
                log.error("Error checking learner content access for: {}", uuid, e);
                return false;
            }
        });
    }
}
