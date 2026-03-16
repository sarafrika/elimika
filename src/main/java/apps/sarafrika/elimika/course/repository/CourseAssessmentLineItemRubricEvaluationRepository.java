package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentLineItemRubricEvaluationRepository extends JpaRepository<CourseAssessmentLineItemRubricEvaluation, Long> {

    Optional<CourseAssessmentLineItemRubricEvaluation> findByUuid(UUID uuid);

    Optional<CourseAssessmentLineItemRubricEvaluation> findByLineItemUuidAndEnrollmentUuid(UUID lineItemUuid, UUID enrollmentUuid);

    List<CourseAssessmentLineItemRubricEvaluation> findByEnrollmentUuidAndLineItemUuidIn(UUID enrollmentUuid, Collection<UUID> lineItemUuids);
}
