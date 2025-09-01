package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseBundleCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseBundleCourseRepository extends JpaRepository<CourseBundleCourse, Long>, JpaSpecificationExecutor<CourseBundleCourse> {

    /**
     * Find course bundle course association by UUID.
     *
     * @param uuid the association UUID
     * @return optional course bundle course association
     */
    Optional<CourseBundleCourse> findByUuid(UUID uuid);

    /**
     * Delete course bundle course association by UUID.
     *
     * @param uuid the association UUID
     */
    void deleteByUuid(UUID uuid);

    /**
     * Find all courses in a bundle ordered by sequence.
     *
     * @param bundleUuid the bundle UUID
     * @return list of bundle courses ordered by sequence
     */
    List<CourseBundleCourse> findByBundleUuidOrderBySequenceOrderAsc(UUID bundleUuid);

    /**
     * Find all courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return list of bundle courses
     */
    List<CourseBundleCourse> findByBundleUuid(UUID bundleUuid);

    /**
     * Find paginated courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param pageable pagination information
     * @return page of bundle courses
     */
    Page<CourseBundleCourse> findByBundleUuid(UUID bundleUuid, Pageable pageable);

    /**
     * Find required courses in a bundle ordered by sequence.
     *
     * @param bundleUuid the bundle UUID
     * @return list of required courses
     */
    List<CourseBundleCourse> findByBundleUuidAndIsRequiredTrueOrderBySequenceOrderAsc(UUID bundleUuid);

    /**
     * Find optional courses in a bundle ordered by sequence.
     *
     * @param bundleUuid the bundle UUID
     * @return list of optional courses
     */
    List<CourseBundleCourse> findByBundleUuidAndIsRequiredFalseOrderBySequenceOrderAsc(UUID bundleUuid);

    /**
     * Find bundles that contain a specific course.
     *
     * @param courseUuid the course UUID
     * @return list of bundle associations containing the course
     */
    List<CourseBundleCourse> findByCourseUuid(UUID courseUuid);

    /**
     * Check if a course is already in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     * @return true if course is in bundle
     */
    boolean existsByBundleUuidAndCourseUuid(UUID bundleUuid, UUID courseUuid);

    /**
     * Find specific bundle-course association.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     * @return optional bundle course association
     */
    Optional<CourseBundleCourse> findByBundleUuidAndCourseUuid(UUID bundleUuid, UUID courseUuid);

    /**
     * Count courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return count of courses in bundle
     */
    long countByBundleUuid(UUID bundleUuid);

    /**
     * Count required courses in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return count of required courses
     */
    long countByBundleUuidAndIsRequiredTrue(UUID bundleUuid);

    /**
     * Delete all courses from a bundle.
     *
     * @param bundleUuid the bundle UUID
     */
    void deleteByBundleUuid(UUID bundleUuid);

    /**
     * Delete specific course from bundle.
     *
     * @param bundleUuid the bundle UUID
     * @param courseUuid the course UUID
     */
    void deleteByBundleUuidAndCourseUuid(UUID bundleUuid, UUID courseUuid);

    /**
     * Find the maximum sequence order for a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return maximum sequence order or null if no courses
     */
    @Query("SELECT MAX(cbc.sequenceOrder) FROM CourseBundleCourse cbc WHERE cbc.bundleUuid = :bundleUuid")
    Integer findMaxSequenceOrderByBundleUuid(@Param("bundleUuid") UUID bundleUuid);

    /**
     * Find courses with duplicate sequence orders in a bundle.
     *
     * @param bundleUuid the bundle UUID
     * @return list of courses with duplicate sequences
     */
    @Query("SELECT cbc FROM CourseBundleCourse cbc WHERE cbc.bundleUuid = :bundleUuid " +
           "AND cbc.sequenceOrder IN (SELECT cbc2.sequenceOrder FROM CourseBundleCourse cbc2 " +
           "WHERE cbc2.bundleUuid = :bundleUuid GROUP BY cbc2.sequenceOrder HAVING COUNT(cbc2) > 1)")
    List<CourseBundleCourse> findDuplicateSequenceOrders(@Param("bundleUuid") UUID bundleUuid);

    /**
     * Find course UUIDs in a bundle ordered by sequence.
     *
     * @param bundleUuid the bundle UUID
     * @return list of course UUIDs
     */
    @Query("SELECT cbc.courseUuid FROM CourseBundleCourse cbc WHERE cbc.bundleUuid = :bundleUuid ORDER BY cbc.sequenceOrder ASC")
    List<UUID> findCourseUuidsByBundleUuidOrderBySequence(@Param("bundleUuid") UUID bundleUuid);

    /**
     * Check if all courses in bundle are published.
     *
     * @param bundleUuid the bundle UUID
     * @return true if all courses are published
     */
    @Query("SELECT COUNT(cbc) = 0 FROM CourseBundleCourse cbc " +
           "JOIN Course c ON cbc.courseUuid = c.uuid " +
           "WHERE cbc.bundleUuid = :bundleUuid AND c.status != 'PUBLISHED'")
    boolean areAllCoursesPublished(@Param("bundleUuid") UUID bundleUuid);
}