package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO;
import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Course recommendations ("rules-v2"): personal lists built from the learner's enrolments, progress,
 * skill goals and affiliations, and a non-personal "similar courses" list. See
 * {@code docs/guides/course-recommendations.md}.
 *
 * @author Wilfred Njuguna
 * @version 2.0
 * @since 2026-07-10
 */
public interface CourseRecommendationService {

    /**
     * Personal recommendations requested by the current caller.
     * <ul>
     *   <li>{@code studentUuid} set: that learner's list, for the learner themselves, a guardian whose share
     *       scope is FULL or ACADEMICS, or a platform admin.</li>
     *   <li>otherwise {@code requestedUserUuid}, defaulting to the caller; only a platform admin may name
     *       another user.</li>
     * </ul>
     * Impressions are recorded against the caller.
     *
     * @param surface {@code for_you} (default) or {@code next_steps}
     * @throws org.springframework.security.access.AccessDeniedException for a learner the caller may not see
     * @throws IllegalArgumentException                                   for an unknown surface or both ids
     */
    List<RecommendedCourseDTO> recommendForCaller(UUID requestedUserUuid, UUID studentUuid, String surface, int limit);

    /**
     * Courses similar to a public course, for anyone: co-enrolment neighbours, same categories and
     * "more like this". Not personal.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException when the course is not public
     */
    List<RecommendedCourseDTO> findSimilar(UUID courseUuid, int limit);

    /** Offline leave-last-out evaluation over {@code course_enrollments}. */
    RecommendationEvaluationDTO evaluate();
}
