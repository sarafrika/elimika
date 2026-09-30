package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    List<CoursePrerequisite> findByCourseUuidOrderByIdAsc(UUID courseUuid);

    /**
     * Whether {@code target} is reachable from any of {@code starts} by following prerequisite edges,
     * the starts themselves included. Giving {@code target} the prerequisites {@code starts} closes a
     * cycle exactly when this holds. {@code UNION} (not {@code UNION ALL}) keeps the walk finite even
     * over bad data.
     * <p>
     * A shadow draft is never anyone's prerequisite, so a pending edit's rows are never walked: only the
     * live graph counts.
     */
    @Query(value = """
            WITH RECURSIVE reach(course_uuid) AS (
                SELECT c.uuid FROM courses c WHERE c.uuid IN (:starts)
                UNION
                SELECT cp.prerequisite_course_uuid
                FROM course_prerequisites cp
                JOIN reach r ON r.course_uuid = cp.course_uuid
            )
            SELECT EXISTS (SELECT 1 FROM reach WHERE course_uuid = :target)
            """, nativeQuery = true)
    boolean reaches(@Param("starts") Collection<UUID> starts, @Param("target") UUID target);
}
