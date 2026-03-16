package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluationRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseAssessmentLineItemRubricEvaluationRowRepository extends JpaRepository<CourseAssessmentLineItemRubricEvaluationRow, Long> {

    List<CourseAssessmentLineItemRubricEvaluationRow> findByEvaluationUuid(UUID evaluationUuid);

    void deleteByEvaluationUuid(UUID evaluationUuid);
}
