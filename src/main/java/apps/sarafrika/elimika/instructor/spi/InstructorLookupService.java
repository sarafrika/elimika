package apps.sarafrika.elimika.instructor.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Instructor Lookup Service Provider Interface
 * <p>
 * Provides read-only access to instructor information for other modules.
 * This interface exposes essential instructor data needed by other modules
 * without giving direct access to Instructor entities or repositories.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface InstructorLookupService {

    /**
     * Finds an instructor UUID by their user UUID.
     *
     * @param userUuid The UUID of the user
     * @return Optional containing the instructor UUID, or empty if not found
     */
    Optional<UUID> findInstructorUuidByUserUuid(UUID userUuid);

    /**
     * Checks if an instructor exists.
     *
     * @param instructorUuid The UUID of the instructor
     * @return true if the instructor exists, false otherwise
     */
    boolean instructorExists(UUID instructorUuid);

    /**
     * Gets the user UUID associated with an instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @return Optional containing the user UUID, or empty if instructor not found
     */
    Optional<UUID> getInstructorUserUuid(UUID instructorUuid);

}