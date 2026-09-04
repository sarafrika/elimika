package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proves that a lesson named in a request body is one the caller may put material into.
 * <p>
 * A quiz and an assignment carry their parent lesson as a plain UUID in the payload, so the lesson
 * is chosen by the caller on every create and every update. An endpoint guard can only ask about
 * the material already in the path; it cannot see where the body is about to move it. Without this
 * check a course creator could take an assignment of their own and re-parent it into somebody
 * else's lesson — or create one there outright — and the material would then appear inside a course
 * they have no relationship with, briefs, due dates and all.
 * <p>
 * The question asked is the same one {@code CourseSecurityServiceImpl#canManageAssignment} asks
 * about the material's current course, so an author cannot reach anywhere through the body that
 * they could not already reach through the path.
 */
@Component
@RequiredArgsConstructor
public class MaterialLessonGuard {

    private final LessonRepository lessonRepository;
    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;

    /**
     * Refuses a lesson whose course the caller neither authored nor is approved to train.
     * <p>
     * A null lesson is not a destination and passes: material may legitimately be created without a
     * parent, and material with no lesson resolves to no course, so it grants nobody anything.
     *
     * @param lessonUuid the lesson the body wants the material to hang off, may be null
     * @throws AccessDeniedException when the lesson is unknown or belongs to another party's course
     */
    public void requireManageableLesson(UUID lessonUuid) {
        if (lessonUuid == null || domainSecurityService.isPlatformAdmin()) {
            return;
        }
        boolean manageable = lessonRepository.findCourseViewByUuid(lessonUuid)
                .map(MaterialCourseView::courseUuid)
                .map(courseSecurityService::canManageCourseGradebook)
                .orElse(false);
        if (!manageable) {
            throw new AccessDeniedException("Lesson does not belong to a course you may add material to.");
        }
    }
}
