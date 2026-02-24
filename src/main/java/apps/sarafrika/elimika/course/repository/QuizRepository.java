package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {
    Optional<Quiz> findByUuid(UUID uuid);

    List<Quiz> findByLessonUuidInOrderByCreatedDateAsc(List<UUID> lessonUuids);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
