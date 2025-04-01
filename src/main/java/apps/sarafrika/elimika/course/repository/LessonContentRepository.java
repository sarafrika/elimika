package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonContentRepository extends JpaRepository<LessonContent, Long>, JpaSpecificationExecutor<LessonContent> {
    Optional<LessonContent> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<LessonContent> findByLessonUuid(UUID lessonUuid);

    List<LessonContent> findByLessonUuidOrderByDisplayOrderAsc(UUID lessonUuid);
}
