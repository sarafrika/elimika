package apps.sarafrika.elimika.coursecreator.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Course Creator Lookup Service Provider Interface
 * <p>
 * Provides read-only access to course creator information for other modules.
 * This interface exposes essential course creator data needed by other modules
 * without giving direct access to CourseCreator entities or repositories.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface CourseCreatorLookupService {

    /**
     * Finds a course creator UUID by their user UUID.
     *
     * @param userUuid The UUID of the user
     * @return Optional containing the course creator UUID, or empty if not found
     */
    Optional<UUID> findCourseCreatorUuidByUserUuid(UUID userUuid);

    /**
     * Checks if a course creator exists.
     *
     * @param courseCreatorUuid The UUID of the course creator
     * @return true if the course creator exists, false otherwise
     */
    boolean courseCreatorExists(UUID courseCreatorUuid);

    /**
     * Gets the user UUID associated with a course creator.
     *
     * @param courseCreatorUuid The UUID of the course creator
     * @return Optional containing the user UUID, or empty if course creator not found
     */
    Optional<UUID> getCourseCreatorUserUuid(UUID courseCreatorUuid);
}