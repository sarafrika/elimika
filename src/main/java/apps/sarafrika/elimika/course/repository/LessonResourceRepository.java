package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Long>, JpaSpecificationExecutor<LessonResource> {

    Optional<LessonResource> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<LessonResource> findByLessonUuid(UUID lessonUuid);

    List<LessonResource> findByLessonUuidOrderByDisplayOrderAsc(UUID lessonUuid);
}
