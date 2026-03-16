package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentLineItemScoreRepository extends JpaRepository<CourseAssessmentLineItemScore, Long> {

    Optional<CourseAssessmentLineItemScore> findByUuid(UUID uuid);

    Optional<CourseAssessmentLineItemScore> findByLineItemUuidAndEnrollmentUuid(UUID lineItemUuid, UUID enrollmentUuid);

    List<CourseAssessmentLineItemScore> findByEnrollmentUuidAndLineItemUuidIn(UUID enrollmentUuid, Collection<UUID> lineItemUuids);

    List<CourseAssessmentLineItemScore> findByLineItemUuid(UUID lineItemUuid);
}
