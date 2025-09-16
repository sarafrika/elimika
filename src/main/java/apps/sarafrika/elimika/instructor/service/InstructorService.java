package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface InstructorService {
    InstructorDTO createInstructor(InstructorDTO instructorDTO);
    InstructorDTO getInstructorByUuid(UUID uuid);
    Page<InstructorDTO> getAllInstructors(Pageable pageable);
    InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO);
    void deleteInstructor(UUID uuid);
    Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ================================
    // INSTRUCTOR VERIFICATION
    // ================================

    /**
     * Verifies/approves an instructor. Only system admins can perform this operation.
     * Sets the admin_verified flag to true for the instructor.
     *
     * @param instructorUuid the instructor UUID to verify
     * @param reason optional reason for verification
     * @return the updated instructor
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if instructor not found
     */
    InstructorDTO verifyInstructor(UUID instructorUuid, String reason);

    /**
     * Removes verification from an instructor. Only system admins can perform this operation.
     * Sets the admin_verified flag to false for the instructor.
     *
     * @param instructorUuid the instructor UUID to unverify
     * @param reason optional reason for removing verification
     * @return the updated instructor
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if instructor not found
     */
    InstructorDTO unverifyInstructor(UUID instructorUuid, String reason);

    /**
     * Checks if an instructor is verified by an admin.
     *
     * @param instructorUuid the instructor UUID
     * @return true if the instructor is admin verified
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if instructor not found
     */
    boolean isInstructorVerified(UUID instructorUuid);

    /**
     * Gets all verified instructors with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of verified instructors
     */
    Page<InstructorDTO> getVerifiedInstructors(Pageable pageable);

    /**
     * Gets all unverified instructors with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of unverified instructors
     */
    Page<InstructorDTO> getUnverifiedInstructors(Pageable pageable);

    /**
     * Gets count of instructors by verification status.
     *
     * @param verified the verification status to count (true for verified, false for unverified)
     * @return count of instructors with the specified verification status
     */
    long countInstructorsByVerificationStatus(boolean verified);
}
