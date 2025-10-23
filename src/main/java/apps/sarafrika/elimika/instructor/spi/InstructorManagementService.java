package apps.sarafrika.elimika.instructor.spi;

import java.util.UUID;

/**
 * Instructor Management Service Provider Interface
 * <p>
 * Provides instructor management operations for other modules.
 * This interface exposes essential instructor management functionality
 * without giving direct access to Instructor entities, repositories, or internal services.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface InstructorManagementService {

    /**
     * Verifies an instructor. Only system admins can perform this operation.
     * Sets the admin_verified flag to true for the instructor.
     *
     * @param instructorUuid The instructor UUID to verify
     * @param reason Optional reason for verification
     * @return The updated instructor data
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if instructor not found
     */
    InstructorDTO verifyInstructor(UUID instructorUuid, String reason);

    /**
     * Removes verification from an instructor. Only system admins can perform this operation.
     * Sets the admin_verified flag to false for the instructor.
     *
     * @param instructorUuid The instructor UUID to unverify
     * @param reason Optional reason for removing verification
     * @return The updated instructor data
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if instructor not found
     */
    InstructorDTO unverifyInstructor(UUID instructorUuid, String reason);

    /**
     * Checks if an instructor is verified by an admin.
     *
     * @param instructorUuid The instructor UUID
     * @return true if the instructor is verified, false otherwise
     */
    boolean isInstructorVerified(UUID instructorUuid);

    /**
     * Counts instructors by verification status.
     *
     * @param verified true for verified instructors, false for pending verification
     * @return number of instructors matching the verification status
     */
    long countInstructorsByVerificationStatus(boolean verified);
}
