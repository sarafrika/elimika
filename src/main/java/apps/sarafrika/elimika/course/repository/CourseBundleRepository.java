package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseBundle;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
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
public interface CourseBundleRepository extends JpaRepository<CourseBundle, Long>, JpaSpecificationExecutor<CourseBundle> {

    /**
     * Find course bundle by UUID.
     *
     * @param uuid the bundle UUID
     * @return optional course bundle
     */
    Optional<CourseBundle> findByUuid(UUID uuid);

    /**
     * Delete course bundle by UUID.
     *
     * @param uuid the bundle UUID
     */
    void deleteByUuid(UUID uuid);

    /**
     * Find all course bundles by instructor UUID.
     *
     * @param instructorUuid the instructor UUID
     * @return list of course bundles
     */
    List<CourseBundle> findByInstructorUuid(UUID instructorUuid);

    /**
     * Find paginated course bundles by instructor UUID.
     *
     * @param instructorUuid the instructor UUID
     * @param pageable pagination information
     * @return page of course bundles
     */
    Page<CourseBundle> findByInstructorUuid(UUID instructorUuid, Pageable pageable);

    /**
     * Find course bundles by status.
     *
     * @param status the content status
     * @param pageable pagination information
     * @return page of course bundles
     */
    Page<CourseBundle> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * Find published and active course bundles.
     *
     * @param pageable pagination information
     * @return page of published and active bundles
     */
    Page<CourseBundle> findByStatusAndActiveTrue(ContentStatus status, Pageable pageable);

    /**
     * Find course bundles by instructor and status.
     *
     * @param instructorUuid the instructor UUID
     * @param status the content status
     * @param pageable pagination information
     * @return page of course bundles
     */
    Page<CourseBundle> findByInstructorUuidAndStatus(UUID instructorUuid, ContentStatus status, Pageable pageable);

    /**
     * Count course bundles by instructor UUID.
     *
     * @param instructorUuid the instructor UUID
     * @return count of bundles
     */
    long countByInstructorUuid(UUID instructorUuid);

    /**
     * Count published course bundles by instructor UUID.
     *
     * @param instructorUuid the instructor UUID
     * @return count of published bundles
     */
    long countByInstructorUuidAndStatus(UUID instructorUuid, ContentStatus status);

    /**
     * Check if bundle name exists for instructor (case-insensitive).
     *
     * @param instructorUuid the instructor UUID
     * @param name the bundle name
     * @return true if name exists
     */
    @Query("SELECT COUNT(cb) > 0 FROM CourseBundle cb WHERE cb.instructorUuid = :instructorUuid AND LOWER(cb.name) = LOWER(:name)")
    boolean existsByInstructorUuidAndNameIgnoreCase(@Param("instructorUuid") UUID instructorUuid, @Param("name") String name);

    /**
     * Check if bundle name exists for instructor excluding current bundle (case-insensitive).
     *
     * @param instructorUuid the instructor UUID
     * @param name the bundle name
     * @param excludeUuid the bundle UUID to exclude
     * @return true if name exists
     */
    @Query("SELECT COUNT(cb) > 0 FROM CourseBundle cb WHERE cb.instructorUuid = :instructorUuid AND LOWER(cb.name) = LOWER(:name) AND cb.uuid != :excludeUuid")
    boolean existsByInstructorUuidAndNameIgnoreCaseAndUuidNot(@Param("instructorUuid") UUID instructorUuid, @Param("name") String name, @Param("excludeUuid") UUID excludeUuid);

    /**
     * Find bundles that are ready for publishing (have at least one course).
     *
     * @param instructorUuid the instructor UUID
     * @return list of bundles ready for publishing
     */
    @Query("SELECT DISTINCT cb FROM CourseBundle cb " +
           "JOIN CourseBundleCourse cbc ON cb.uuid = cbc.bundleUuid " +
           "WHERE cb.instructorUuid = :instructorUuid AND cb.status = 'DRAFT'")
    List<CourseBundle> findBundlesReadyForPublishing(@Param("instructorUuid") UUID instructorUuid);

    /**
     * Find published bundles with search criteria.
     *
     * @param searchTerm the search term for name or description
     * @param pageable pagination information
     * @return page of matching bundles
     */
    @Query("SELECT cb FROM CourseBundle cb WHERE cb.status = 'PUBLISHED' AND cb.active = true " +
           "AND (LOWER(cb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cb.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<CourseBundle> findPublishedBundlesWithSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
}