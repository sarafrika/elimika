package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.OrgInstructorSummaryDTO;
import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.shared.search.NearMe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InstructorService {
    InstructorDTO createInstructor(InstructorDTO instructorDTO);
    InstructorDTO getInstructorByUuid(UUID uuid);
    /**
     * Lists instructors. With a {@code q}, matches it against the directory: through the search index
     * when reads are enabled for it, otherwise as a case-insensitive full-name match.
     */
    Page<InstructorDTO> getAllInstructors(String q, Pageable pageable);
    InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO);
    void deleteInstructor(UUID uuid);
    /**
     * Filters instructors by {@code searchParams}. A {@code q} key is free text, served like
     * {@link #getAllInstructors(String, Pageable)}; every other key keeps its filter meaning.
     */
    Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Near-me: verified instructors who opted in, within {@code near}'s radius, each with a
     * {@code distance_band}. Nearest first without {@code q}, by relevance with it. Served only by the
     * instructors index (503 when it cannot answer); coordinates in the rows are rounded to 2 decimals.
     */
    Page<InstructorDTO> searchNear(String q, NearMe near, Map<String, String> searchParams, Pageable pageable);

    /**
     * Turns the owner's near-me opt-in on or off; the profile is re-indexed with or without its
     * rounded location.
     */
    InstructorDTO setLocationSearchOptIn(UUID uuid, boolean enabled);

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

    /**
     * Returns aggregated instructor directory rows scoped to a single organisation.
     *
     * @param organisationUuid the organisation to scope to
     * @return one summary row per active instructor in the organisation
     */
    List<OrgInstructorSummaryDTO> getInstructorSummariesForOrganisation(UUID organisationUuid);
}
