package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Recommends published courses to a user based on their past courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-10
 */
public interface CourseRecommendationService {

    /**
     * Returns up to {@code limit} recommended published courses for the given user,
     * ranked by topic and level overlap with the user's past courses. Falls back to
     * the most recently published courses when the user has no usable history.
     *
     * @param userUuid the user to recommend for
     * @param limit    the maximum number of recommendations (defaults/caps applied)
     * @return an ordered list of recommendations, strongest match first
     */
    List<RecommendedCourseDTO> recommendForUser(UUID userUuid, int limit);
}
