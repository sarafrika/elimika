package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    /**
     * Checks if a course exists with the given name (case-insensitive).
     *
     * @param name the name to check
     * @return true if a course with the given name exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if a course exists with the given code (case-insensitive).
     *
     * @param code the code to check
     * @return true if a course with the given code exists, false otherwise
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Finds a course by its UUID.
     *
     * @param uuid the UUID to look for
     * @return an Optional containing the course if found, or empty if not found
     */
    Optional<Course> findByUuid(UUID uuid);
}
