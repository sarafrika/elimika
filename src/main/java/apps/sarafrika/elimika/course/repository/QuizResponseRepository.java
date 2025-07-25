package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.QuizResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizResponseRepository extends JpaRepository<QuizResponse, Long>, JpaSpecificationExecutor<QuizResponse> {
    Optional<QuizResponse> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    Optional<QuizResponse> findByAttemptUuid(UUID attemptUuid);

    boolean existsByUuid(UUID uuid);

    long countByQuestionUuid(UUID questionUuid);
}
