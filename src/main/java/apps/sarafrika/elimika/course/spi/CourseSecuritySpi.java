package apps.sarafrika.elimika.course.spi;

import java.util.UUID;

/**
 * Service Provider Interface for course-related security operations.
 * This interface provides authorization checks for course ownership.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
public interface CourseSecuritySpi {

    /**
     * Checks if the currently authenticated user is the owner of the specified course.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user owns the course, false otherwise
     */
    boolean isCourseOwner(UUID courseUuid);

    /**
     * Checks whether the current user may read the course's lesson content.
     * <p>
     * True for the course owner or a member of an organisation approved to train
     * the course. Platform admins are granted separately at the endpoint. Enrolled
     * learners read content through their own class flow, not this path.
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user may read the course content, false otherwise
     */
    boolean canReadCourseContent(UUID courseUuid);
}
