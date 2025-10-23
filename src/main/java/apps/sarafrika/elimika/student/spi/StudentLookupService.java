package apps.sarafrika.elimika.student.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Student Lookup Service Provider Interface
 * <p>
 * Provides read-only access to student information for other modules.
 * This interface exposes essential student data needed by other modules
 * without giving direct access to Student entities or repositories.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface StudentLookupService {

    /**
     * Finds a student UUID by their user UUID.
     *
     * @param userUuid The UUID of the user
     * @return Optional containing the student UUID, or empty if not found
     */
    Optional<UUID> findStudentUuidByUserUuid(UUID userUuid);

    /**
     * Checks if a student exists.
     *
     * @param studentUuid The UUID of the student
     * @return true if the student exists, false otherwise
     */
    boolean studentExists(UUID studentUuid);

    /**
     * Gets the user UUID associated with a student.
     *
     * @param studentUuid The UUID of the student
     * @return Optional containing the user UUID, or empty if student not found
     */
    Optional<UUID> getStudentUserUuid(UUID studentUuid);

}