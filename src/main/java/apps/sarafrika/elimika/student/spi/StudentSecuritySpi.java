package apps.sarafrika.elimika.student.spi;

import java.util.UUID;

/**
 * Service Provider Interface for student-related security operations.
 * This interface provides authorization checks for student identity.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
public interface StudentSecuritySpi {

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     *
     * @param studentUuid UUID of the student to check
     * @return true if the current user is the specified student, false otherwise
     */
    boolean isStudentWithUuid(UUID studentUuid);
}
