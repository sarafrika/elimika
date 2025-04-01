package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing CourseCategory entities.
 */
@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, UUID>, JpaSpecificationExecutor<CourseCategory> {

    /**
     * Finds a course category by its UUID.
     *
     * @param uuid The UUID to search for.
     * @return An Optional containing the course category if found.
     */
    Optional<CourseCategory> findByUuid(UUID uuid);

    /**
     * Finds all course categories associated with a specific course.
     *
     * @param courseUuid The UUID of the course.
     * @return A list of CourseCategory entities.
     */
    List<CourseCategory> findByCourseUuid(UUID courseUuid);

    /**
     * Finds all course categories associated with a specific category.
     *
     * @param categoryUuid The UUID of the category.
     * @return A list of CourseCategory entities.
     */
    List<CourseCategory> findByCategoryUuid(UUID categoryUuid);

    /**
     * Checks if an association exists between a course and a category.
     *
     * @param courseUuid The UUID of the course.
     * @param categoryUuid The UUID of the category.
     * @return True if the association exists, false otherwise.
     */
    boolean existsByCourseUuidAndCategoryUuid(UUID courseUuid, UUID categoryUuid);

    /**
     * Deletes all course categories associated with a specific course.
     *
     * @param courseUuid The UUID of the course.
     */
    @Modifying
    @Query("DELETE FROM CourseCategory cc WHERE cc.courseUuid = :courseUuid")
    void deleteByCourseUuid(@Param("courseUuid") UUID courseUuid);

    /**
     * Deletes all course categories associated with a specific category.
     *
     * @param categoryUuid The UUID of the category.
     */
    @Modifying
    @Query("DELETE FROM CourseCategory cc WHERE cc.categoryUuid = :categoryUuid")
    void deleteByCategoryUuid(@Param("categoryUuid") UUID categoryUuid);

    /**
     * Counts how many courses are associated with a specific category.
     *
     * @param categoryUuid The UUID of the category.
     * @return The count of associated courses.
     */
    long countByCategoryUuid(UUID categoryUuid);
}