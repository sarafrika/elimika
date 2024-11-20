package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l JOIN Course c on l.courseId = c.id WHERE c.id = :courseId AND l.id = :lessonId")
    Optional<Lesson> findByCourseIdAndLessonId(@Param("courseId") Long courseId, @Param("lessonId") Long lessonId);

    @Query("SELECT l FROM Lesson l JOIN Course c ON l.courseId = c.id WHERE c.id = :courseId")
    Page<Lesson> findAllByCourseId(@Param("courseId") Long courseId, Pageable pageable);

}
