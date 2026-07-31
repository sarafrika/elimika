package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    Optional<Lesson> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<Lesson> findByCourseUuidOrderByLessonNumberAsc(UUID courseUuid);

    /**
     * Resolves the owning course and publish state of a lesson in one query, matching the shape used
     * for quizzes and assignments.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.MaterialCourseView(
                       l.courseUuid, l.status, l.active, null)
            FROM Lesson l
            WHERE l.uuid = :lessonUuid
            """)
    Optional<MaterialCourseView> findCourseViewByUuid(@Param("lessonUuid") UUID lessonUuid);

    /**
     * UUIDs of the learner-visible lessons across a set of courses, for force-scoping assessment
     * searches to material the caller is entitled to see.
     */
    @Query("""
            SELECT l.uuid FROM Lesson l
            WHERE l.courseUuid IN :courseUuids AND l.status = :status AND l.active = true
            """)
    List<UUID> findVisibleLessonUuidsByCourseUuidIn(@Param("courseUuids") Collection<UUID> courseUuids,
                                                    @Param("status") ContentStatus status);
}