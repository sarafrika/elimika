package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentScoreRepository extends JpaRepository<CourseAssessmentScore, Long>,
        JpaSpecificationExecutor<CourseAssessmentScore> {
    Optional<CourseAssessmentScore> findByUuid(UUID uuid);

    Optional<CourseAssessmentScore> findByEnrollmentUuidAndAssessmentUuid(UUID enrollmentUuid, UUID assessmentUuid);

    List<CourseAssessmentScore> findByEnrollmentUuidAndAssessmentUuidIn(UUID enrollmentUuid, Collection<UUID> assessmentUuids);

    void deleteByUuid(UUID uuid);

    void deleteByEnrollmentUuidAndAssessmentUuid(UUID enrollmentUuid, UUID assessmentUuid);

    boolean existsByUuid(UUID uuid);
}
