package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseBundleDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CourseBundleService {
    
    /**
     * Create a new course bundle.
     *
     * @param courseBundleDTO the bundle data
     * @return created bundle DTO
     */
    CourseBundleDTO createCourseBundle(CourseBundleDTO courseBundleDTO);

    /**
     * Get course bundle by UUID.
     *
     * @param uuid the bundle UUID
     * @return bundle DTO
     */
    CourseBundleDTO getCourseBundleByUuid(UUID uuid);

    /**
     * Get all course bundles with pagination.
     *
     * @param pageable pagination information
     * @return page of bundle DTOs
     */
    Page<CourseBundleDTO> getAllCourseBundles(Pageable pageable);

    /**
     * Update course bundle.
     *
     * @param uuid the bundle UUID
     * @param courseBundleDTO the updated bundle data
     * @return updated bundle DTO
     */
    CourseBundleDTO updateCourseBundle(UUID uuid, CourseBundleDTO courseBundleDTO);

    /**
     * Delete course bundle.
     *
     * @param uuid the bundle UUID
     */
    void deleteCourseBundle(UUID uuid);

    /**
     * Search course bundles.
     *
     * @param searchParams search parameters
     * @param pageable pagination information
     * @return page of matching bundle DTOs
     */
    Page<CourseBundleDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Get course bundles by instructor.
     *
     * @param instructorUuid the instructor UUID
     * @param pageable pagination information
     * @return page of instructor's bundles
     */
    Page<CourseBundleDTO> getCourseBundlesByInstructor(UUID instructorUuid, Pageable pageable);

    /**
     * Get published and active course bundles.
     *
     * @param pageable pagination information
     * @return page of published bundles
     */
    Page<CourseBundleDTO> getPublishedCourseBundles(Pageable pageable);

    /**
     * Get courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return list of courses in the bundle
     */
    List<CourseDTO> getBundleCourses(UUID bundleUuid);

    /**
     * Add course to bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     * @param sequenceOrder optional sequence order
     * @param isRequired whether course is required
     * @return updated bundle DTO
     */
    CourseBundleDTO addCourseToBundle(UUID bundleUuid, UUID courseUuid, Integer sequenceOrder, Boolean isRequired);

    /**
     * Remove course from bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     * @return updated bundle DTO
     */
    CourseBundleDTO removeCourseFromBundle(UUID bundleUuid, UUID courseUuid);

    // Lifecycle management methods
    
    /**
     * Check if bundle is ready for publishing.
     *
     * @param uuid the bundle UUID
     * @return true if ready for publishing
     */
    boolean isBundleReadyForPublishing(UUID uuid);

    /**
     * Publish course bundle.
     *
     * @param uuid the bundle UUID
     * @return published bundle DTO
     */
    CourseBundleDTO publishBundle(UUID uuid);

    /**
     * Unpublish course bundle.
     *
     * @param uuid the bundle UUID
     * @return unpublished bundle DTO
     */
    CourseBundleDTO unpublishBundle(UUID uuid);

    /**
     * Archive course bundle.
     *
     * @param uuid the bundle UUID
     * @return archived bundle DTO
     */
    CourseBundleDTO archiveBundle(UUID uuid);

    /**
     * Check if bundle can be unpublished.
     *
     * @param uuid the bundle UUID
     * @return true if can be unpublished
     */
    boolean canUnpublishBundle(UUID uuid);

    /**
     * Get available status transitions for bundle.
     *
     * @param uuid the bundle UUID
     * @return list of available status transitions
     */
    List<ContentStatus> getAvailableStatusTransitions(UUID uuid);

    /**
     * Validate bundle content.
     *
     * @param uuid the bundle UUID
     * @return true if bundle content is valid
     */
    boolean validateBundleContent(UUID uuid);

    // Media upload methods
    
    /**
     * Upload bundle thumbnail.
     *
     * @param bundleUuid the bundle UUID
     * @param thumbnail the thumbnail file
     * @return updated bundle DTO
     */
    CourseBundleDTO uploadThumbnail(UUID bundleUuid, MultipartFile thumbnail);

    /**
     * Upload bundle banner.
     *
     * @param bundleUuid the bundle UUID
     * @param banner the banner file
     * @return updated bundle DTO
     */
    CourseBundleDTO uploadBanner(UUID bundleUuid, MultipartFile banner);
}