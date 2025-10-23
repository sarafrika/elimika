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
}
