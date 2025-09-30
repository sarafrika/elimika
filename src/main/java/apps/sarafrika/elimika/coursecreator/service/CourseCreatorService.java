package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorService {
    CourseCreatorDTO createCourseCreator(CourseCreatorDTO courseCreatorDTO);
    CourseCreatorDTO getCourseCreatorByUuid(UUID uuid);
    Page<CourseCreatorDTO> getAllCourseCreators(Pageable pageable);
    CourseCreatorDTO updateCourseCreator(UUID uuid, CourseCreatorDTO courseCreatorDTO);
    void deleteCourseCreator(UUID uuid);
    Page<CourseCreatorDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ================================
    // COURSE CREATOR VERIFICATION
    // ================================

    /**
     * Verifies/approves a course creator. Only system admins can perform this operation.
     * Sets the admin_verified flag to true for the course creator.
     *
     * @param courseCreatorUuid the course creator UUID to verify
     * @param reason optional reason for verification
     * @return the updated course creator
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if course creator not found
     */
    CourseCreatorDTO verifyCourseCreator(UUID courseCreatorUuid, String reason);

    /**
     * Removes verification from a course creator. Only system admins can perform this operation.
     * Sets the admin_verified flag to false for the course creator.
     *
     * @param courseCreatorUuid the course creator UUID to unverify
     * @param reason optional reason for removing verification
     * @return the updated course creator
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if course creator not found
     */
    CourseCreatorDTO unverifyCourseCreator(UUID courseCreatorUuid, String reason);

    /**
     * Checks if a course creator is verified by an admin.
     *
     * @param courseCreatorUuid the course creator UUID
     * @return true if the course creator is admin verified
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if course creator not found
     */
    boolean isCourseCreatorVerified(UUID courseCreatorUuid);

    /**
     * Gets all verified course creators with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of verified course creators
     */
    Page<CourseCreatorDTO> getVerifiedCourseCreators(Pageable pageable);

    /**
     * Gets all unverified course creators with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of unverified course creators
     */
    Page<CourseCreatorDTO> getUnverifiedCourseCreators(Pageable pageable);

    /**
     * Gets count of course creators by verification status.
     *
     * @param verified the verification status to count (true for verified, false for unverified)
     * @return count of course creators with the specified verification status
     */
    long countCourseCreatorsByVerificationStatus(boolean verified);
}