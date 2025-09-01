package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseBundleCourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CourseBundleCourseService {
    
    /**
     * Create a bundle-course association.
     *
     * @param courseBundleCourseDTO the association data
     * @return created association DTO
     */
    CourseBundleCourseDTO createBundleCourseAssociation(CourseBundleCourseDTO courseBundleCourseDTO);

    /**
     * Get bundle-course association by UUID.
     *
     * @param uuid the association UUID
     * @return association DTO
     */
    CourseBundleCourseDTO getBundleCourseAssociationByUuid(UUID uuid);

    /**
     * Get all bundle-course associations with pagination.
     *
     * @param pageable pagination information
     * @return page of association DTOs
     */
    Page<CourseBundleCourseDTO> getAllBundleCourseAssociations(Pageable pageable);

    /**
     * Update bundle-course association.
     *
     * @param uuid the association UUID
     * @param courseBundleCourseDTO the updated association data
     * @return updated association DTO
     */
    CourseBundleCourseDTO updateBundleCourseAssociation(UUID uuid, CourseBundleCourseDTO courseBundleCourseDTO);

    /**
     * Delete bundle-course association.
     *
     * @param uuid the association UUID
     */
    void deleteBundleCourseAssociation(UUID uuid);

    /**
     * Search bundle-course associations.
     *
     * @param searchParams search parameters
     * @param pageable pagination information
     * @return page of matching association DTOs
     */
    Page<CourseBundleCourseDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Get courses in a bundle ordered by sequence.
     *
     * @param bundleUuid the bundle UUID
     * @return list of bundle-course associations
     */
    List<CourseBundleCourseDTO> getCoursesByBundle(UUID bundleUuid);

    /**
     * Get required courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return list of required course associations
     */
    List<CourseBundleCourseDTO> getRequiredCoursesByBundle(UUID bundleUuid);

    /**
     * Get optional courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return list of optional course associations
     */
    List<CourseBundleCourseDTO> getOptionalCoursesByBundle(UUID bundleUuid);

    /**
     * Get bundles containing a specific course.
     *
     * @param courseUuid the course UUID
     * @return list of bundle associations containing the course
     */
    List<CourseBundleCourseDTO> getBundlesByCourse(UUID courseUuid);

    /**
     * Check if course is in bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     * @return true if course is in bundle
     */
    boolean isCourseInBundle(UUID bundleUuid, UUID courseUuid);

    /**
     * Get course count in bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return number of courses in bundle
     */
    long getCourseCountInBundle(UUID bundleUuid);

    /**
     * Reorder courses in bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseOrder map of course UUID to sequence order
     */
    void reorderCoursesInBundle(UUID bundleUuid, Map<UUID, Integer> courseOrder);

    /**
     * Remove all courses from bundle.
     *
     * @param bundleUuid the bundle UUID
     */
    void removeAllCoursesFromBundle(UUID bundleUuid);
}