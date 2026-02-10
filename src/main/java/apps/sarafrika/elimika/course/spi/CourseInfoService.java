package apps.sarafrika.elimika.course.spi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Course Information Service Provider Interface
 * <p>
 * Provides read-only access to course information for other modules.
 * This interface exposes essential course data needed by other modules
 * without giving direct access to the Course entity or repository.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface CourseInfoService {

    /**
     * Retrieves the minimum training fee for a course.
     *
     * @param courseUuid The UUID of the course
     * @return Optional containing the minimum training fee, or empty if course not found or fee not set
     */
    Optional<BigDecimal> getMinimumTrainingFee(UUID courseUuid);

    /**
     * Checks if a course exists.
     *
     * @param courseUuid The UUID of the course
     * @return true if the course exists, false otherwise
     */
    boolean courseExists(UUID courseUuid);

    /**
     * Gets the course name.
     *
     * @param courseUuid The UUID of the course
     * @return Optional containing the course name, or empty if course not found
     */
    Optional<String> getCourseName(UUID courseUuid);

    /**
     * Retrieves the configured age limits for a course, if any.
     *
     * @param courseUuid The UUID of the course
     * @return Optional containing minimum/maximum age, or empty when no limits exist
     */
    Optional<AgeLimits> getAgeLimits(UUID courseUuid);

    /**
     * Retrieves the revenue share percentages for a course.
     *
     * @param courseUuid The UUID of the course
     * @return Optional containing the revenue share percentages, or empty if course not found
     */
    Optional<RevenueShare> getRevenueShare(UUID courseUuid);

    /**
     * Retrieves revenue share percentages for multiple courses.
     *
     * @param courseUuids The course UUIDs to fetch
     * @return Map of course UUID to revenue share percentages
     */
    Map<UUID, RevenueShare> getRevenueShares(List<UUID> courseUuids);

    /**
     * Checks if a training program exists.
     *
     * @param programUuid The UUID of the training program
     * @return true if the training program exists, false otherwise
     */
    boolean trainingProgramExists(UUID programUuid);

    /**
     * Gets the training program title.
     *
     * @param programUuid The UUID of the training program
     * @return Optional containing the training program title, or empty if not found
     */
    Optional<String> getTrainingProgramTitle(UUID programUuid);

    /**
     * Retrieves course UUIDs owned by a course creator.
     *
     * @param courseCreatorUuid The UUID of the course creator
     * @return List of course UUIDs owned by the creator
     */
    List<UUID> findCourseUuidsByCourseCreatorUuid(UUID courseCreatorUuid);

    record AgeLimits(Integer minAge, Integer maxAge) { }

    record RevenueShare(BigDecimal creatorSharePercentage, BigDecimal instructorSharePercentage) { }
}
