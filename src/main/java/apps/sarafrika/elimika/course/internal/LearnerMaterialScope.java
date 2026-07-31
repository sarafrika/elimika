package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Narrows a search over course material to what the calling learner is entitled to see.
 * <p>
 * The assessment search endpoints accept a free-form {@code Map<String, String>} and translate it
 * into a specification over <em>every</em> row in the table. That is fine for staff and unacceptable
 * for a learner: without a floor, opening those endpoints would hand any student the platform's
 * whole question bank. This component supplies the floor — material must be published, and it must
 * belong to a course the caller is enrolled in.
 * <p>
 * The lesson filter is expressed as an {@code IN} over pre-resolved lesson UUIDs rather than a
 * correlated subquery. A learner has a handful of courses and tens of lessons, so the list is small,
 * it is loaded once per request, and it keeps the predicate simple enough to assert directly in a
 * unit test.
 */
@Component
@RequiredArgsConstructor
public class LearnerMaterialScope {

    private static final String CACHE_VISIBLE_LESSONS = "learnerMaterial.visibleLessonUuids";

    private final LessonRepository lessonRepository;
    private final CourseSecuritySpi courseSecurityService;
    private final LearnerAssessmentScope learnerAssessmentScope;
    private final RequestScopedCache requestScopedCache;

    /**
     * Lesson UUIDs the calling learner may see: published and active lessons of the courses they are
     * enrolled in. Empty when they are enrolled in nothing, which fails closed.
     */
    public Set<UUID> visibleLessonUuids() {
        return requestScopedCache.get(CACHE_VISIBLE_LESSONS, () -> {
            Set<UUID> courseUuids = courseSecurityService.enrolledCourseUuids();
            if (courseUuids.isEmpty()) {
                return Set.<UUID>of();
            }
            List<UUID> lessonUuids = lessonRepository.findVisibleLessonUuidsByCourseUuidIn(
                    courseUuids, ContentStatus.PUBLISHED);
            return Set.copyOf(lessonUuids);
        });
    }

    /**
     * Restricts a quiz search to published, active quizzes on the caller's enrolled courses. Staff
     * searches pass through untouched.
     */
    public <T> Specification<T> restrictQuizzes(Specification<T> base) {
        return restrict(base, (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ContentStatus.PUBLISHED),
                cb.isTrue(root.get("active"))));
    }

    /**
     * Restricts an assignment search to published assignments on the caller's enrolled courses.
     * Staff searches pass through untouched.
     */
    public <T> Specification<T> restrictAssignments(Specification<T> base) {
        return restrict(base, (root, query, cb) -> cb.isTrue(root.get("isPublished")));
    }

    /**
     * ANDs the caller's entitlement onto a search: the material's own publish rule, plus membership
     * of a lesson they may see. Anything not reachable that way is simply absent from the results
     * rather than an error, which is how a search should behave.
     */
    private <T> Specification<T> restrict(Specification<T> base, Specification<T> publishedRule) {
        if (learnerAssessmentScope.seesAllLearners()) {
            return base;
        }

        Set<UUID> lessonUuids = visibleLessonUuids();
        Specification<T> entitled = lessonUuids.isEmpty()
                ? (root, query, cb) -> cb.disjunction()
                : publishedRule.and((root, query, cb) -> root.get("lessonUuid").in(lessonUuids));

        return base == null ? entitled : base.and(entitled);
    }
}
