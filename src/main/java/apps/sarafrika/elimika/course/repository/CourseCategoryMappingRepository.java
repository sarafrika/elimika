package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCategoryMappingRepository extends JpaRepository<CourseCategoryMapping, Long>,
        JpaSpecificationExecutor<CourseCategoryMapping> {

    /**
     * Find all category mappings for a specific course
     */
    List<CourseCategoryMapping> findByCourseUuid(UUID courseUuid);

    /**
     * Find all course mappings for a specific category
     */
    List<CourseCategoryMapping> findByCategoryUuid(UUID categoryUuid);

    /**
     * Find specific mapping between a course and category
     */
    Optional<CourseCategoryMapping> findByCourseUuidAndCategoryUuid(UUID courseUuid, UUID categoryUuid);

    /**
     * Check if a course-category mapping exists
     */
    boolean existsByCourseUuidAndCategoryUuid(UUID courseUuid, UUID categoryUuid);

    /**
     * Delete all mappings for a specific course
     */
    @Modifying
    void deleteByCourseUuid(UUID courseUuid);

    /**
     * Delete all mappings for a specific category
     */
    @Modifying
    void deleteByCategoryUuid(UUID categoryUuid);

    /**
     * Delete a specific course-category mapping
     */
    @Modifying
    void deleteByCourseUuidAndCategoryUuid(UUID courseUuid, UUID categoryUuid);

    /**
     * Count mappings for a specific course
     */
    long countByCourseUuid(UUID courseUuid);

    /**
     * Count mappings for a specific category
     */
    long countByCategoryUuid(UUID categoryUuid);

    /**
     * Get category names for a specific course
     */
    @Query("""
        SELECT c.name 
        FROM CourseCategoryMapping ccm 
        JOIN Category c ON ccm.categoryUuid = c.uuid 
        WHERE ccm.courseUuid = :courseUuid
        ORDER BY c.name
        """)
    List<String> findCategoryNamesByCourseUuid(@Param("courseUuid") UUID courseUuid);

    /**
     * Get course UUIDs for a specific category
     */
    @Query("SELECT ccm.courseUuid FROM CourseCategoryMapping ccm WHERE ccm.categoryUuid = :categoryUuid")
    List<UUID> findCourseUuidsByCategoryUuid(@Param("categoryUuid") UUID categoryUuid);

    /**
     * Find mappings by UUID
     */
    Optional<CourseCategoryMapping> findByUuid(UUID uuid);

    /**
     * Check if mapping exists by UUID
     */
    boolean existsByUuid(UUID uuid);

    /**
     * Delete mapping by UUID
     */
    @Modifying
    void deleteByUuid(UUID uuid);
}