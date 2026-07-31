package apps.sarafrika.elimika.course.repository.projection;

import apps.sarafrika.elimika.course.util.enums.ContentStatus;

import java.util.UUID;

/**
 * The two facts an authorization check needs about a piece of course material: which course owns it,
 * and whether it is published.
 * <p>
 * Quizzes, assignments and lessons reference their parents by raw UUID rather than by JPA
 * association, so resolving "which course is this quiz in?" naively costs one query per hop. This
 * projection collapses the walk into a single join, and carries the publish state along for free
 * because every learner check needs both.
 *
 * @param courseUuid the course that owns the material
 * @param status     the material's content status, null for material that has no status column
 * @param active     the material's active flag, null for material that has no active column
 * @param published  the material's published flag, null for material that has no published column
 */
public record MaterialCourseView(UUID courseUuid, ContentStatus status, Boolean active, Boolean published) {

    /**
     * Whether learners are allowed to see this material at all. Status-and-active material must be
     * both; published-flag material need only be published.
     */
    public boolean visibleToLearners() {
        if (status != null || active != null) {
            return status == ContentStatus.PUBLISHED && Boolean.TRUE.equals(active);
        }
        return Boolean.TRUE.equals(published);
    }
}
