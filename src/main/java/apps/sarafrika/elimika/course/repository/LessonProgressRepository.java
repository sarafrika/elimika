package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long>, JpaSpecificationExecutor<LessonProgress> {
    Optional<LessonProgress> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}