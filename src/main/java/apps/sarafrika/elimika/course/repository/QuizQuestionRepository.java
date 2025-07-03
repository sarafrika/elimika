package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long>, JpaSpecificationExecutor<QuizQuestion> {
    Optional<QuizQuestion> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<QuizQuestion> findByQuizUuid(UUID queryUuid);

    boolean existsByUuid(UUID uuid);

    List<QuizQuestion> findByQuizUuidOrderByDisplayOrderAsc(UUID quizUuid);

    List<QuizQuestion> findByQuizUuidAndQuestionType(UUID quizUuid, QuestionType questionType);

    int findMaxDisplayOrderByQuizUuid(UUID quizUuid);

    long countByQuizUuid(UUID quizUuid);

    boolean existsByQuizUuidAndQuestionTypeIn(UUID quizUuid, List<QuestionType> questionTypes);

}
