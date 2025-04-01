package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l JOIN Course c on l.courseId = c.id WHERE c.id = :courseId AND l.id = :lessonId")
    Optional<Lesson> findByCourseIdAndLessonId(@Param("courseId") Long courseId, @Param("lessonId") Long lessonId);

    @Query("SELECT l FROM Lesson l JOIN Course c ON l.courseId = c.id WHERE c.id = :courseId")
    Page<Lesson> findAllByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    Optional<Lesson> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<Lesson> findByCourseUuid(UUID courseUuid);

    List<Lesson> findByCourseUuidOrderByLessonOrderAsc(UUID courseUuid);

}
