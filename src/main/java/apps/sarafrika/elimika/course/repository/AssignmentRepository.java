package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>,
        JpaSpecificationExecutor<Assignment> {
    Optional<Assignment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<Assignment> findByLessonUuid(UUID lessonUuid);

    /**
     * Resolves the owning course and publish state of an assignment in one query, for authorization
     * checks that would otherwise walk assignment to lesson to course a row at a time.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.MaterialCourseView(
                       l.courseUuid, null, null, a.isPublished)
            FROM Assignment a JOIN Lesson l ON l.uuid = a.lessonUuid
            WHERE a.uuid = :assignmentUuid
            """)
    Optional<MaterialCourseView> findCourseViewByUuid(@Param("assignmentUuid") UUID assignmentUuid);
}