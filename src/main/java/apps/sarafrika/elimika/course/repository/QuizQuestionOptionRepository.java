package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizQuestionOptionRepository extends JpaRepository<QuizQuestionOption, Long>, JpaSpecificationExecutor<QuizQuestionOption> {
    Optional<QuizQuestionOption> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}