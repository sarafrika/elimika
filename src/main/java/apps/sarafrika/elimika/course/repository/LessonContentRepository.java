package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonContentRepository extends JpaRepository<LessonContent, Long>, JpaSpecificationExecutor<LessonContent> {
    Optional<LessonContent> findByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
}