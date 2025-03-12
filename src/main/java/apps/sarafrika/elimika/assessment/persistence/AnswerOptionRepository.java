package apps.sarafrika.elimika.assessment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findAllByQuestionId(Long questionId);

}
