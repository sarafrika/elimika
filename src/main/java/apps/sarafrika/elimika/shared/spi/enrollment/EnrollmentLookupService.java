package apps.sarafrika.elimika.shared.spi.enrollment;

import java.util.Optional;
import java.util.UUID;

/**
 * Enrollment Lookup Service Provider Interface
 * <p>
 * Provides read-only access to enrollment information for other modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface EnrollmentLookupService {

    /**
     * Checks if an enrollment exists.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return true if the enrollment exists, false otherwise
     */
    boolean enrollmentExists(UUID enrollmentUuid);

    /**
     * Gets the student UUID for an enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return Optional containing the student UUID, or empty if enrollment not found
     */
    Optional<UUID> getEnrollmentStudentUuid(UUID enrollmentUuid);

    /**
     * Gets the scheduled instance UUID for an enrollment.
     *
     * @param enrollmentUuid The UUID of the enrollment
     * @return Optional containing the scheduled instance UUID, or empty if enrollment not found
     */
    Optional<UUID> getEnrollmentScheduledInstanceUuid(UUID enrollmentUuid);

    /**
     * Checks if a student is enrolled in a specific scheduled instance.
     *
     * @param studentUuid The UUID of the student
     * @param scheduledInstanceUuid The UUID of the scheduled instance
     * @return true if the student is enrolled, false otherwise
     */
    boolean isStudentEnrolledInInstance(UUID studentUuid, UUID scheduledInstanceUuid);
}