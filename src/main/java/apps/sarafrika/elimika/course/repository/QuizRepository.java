package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Quiz;
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
public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {
    Optional<Quiz> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<Quiz> findByLessonUuid(UUID lessonUuid);

    /**
     * Resolves the owning course and publish state of a quiz in one query, for authorization checks
     * that would otherwise walk quiz to lesson to course a row at a time.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.MaterialCourseView(
                       l.courseUuid, q.status, q.active, null)
            FROM Quiz q JOIN Lesson l ON l.uuid = q.lessonUuid
            WHERE q.uuid = :quizUuid
            """)
    Optional<MaterialCourseView> findCourseViewByUuid(@Param("quizUuid") UUID quizUuid);
}
