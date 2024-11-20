package apps.sarafrika.elimika.assessment.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findAllByAssessmentId(Long assessmentId, Pageable pageable);

    Optional<Question> findByIdAndAssessmentId(Long id, Long assessmentId);
}
