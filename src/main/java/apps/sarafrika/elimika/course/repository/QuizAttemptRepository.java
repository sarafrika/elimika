package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long>, JpaSpecificationExecutor<QuizAttempt> {
    Optional<QuizAttempt> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}