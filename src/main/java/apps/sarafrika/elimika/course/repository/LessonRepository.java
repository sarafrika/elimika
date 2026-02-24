package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    Optional<Lesson> findByUuid(UUID uuid);

    List<Lesson> findByCourseUuidOrderByLessonNumberAsc(UUID courseUuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
