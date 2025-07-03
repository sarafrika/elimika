package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long>, JpaSpecificationExecutor<AssignmentSubmission> {
    Optional<AssignmentSubmission> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    Optional<AssignmentSubmission> findByAssignmentUuid(UUID assignmentUuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByEnrollmentUuidAndAssignmentUuid(UUID enrollmentUuid, UUID  assignmentUuid);

    List<AssignmentSubmission> findSubmissionsPendingGradingByInstructor(UUID instructorUuid);

    List<AssignmentSubmission> findByAssignmentUuidAndStatus(UUID assignmentUuid, SubmissionStatus status);

    List<AssignmentSubmission> findByStatusAndInstructorCommentsIsNull(SubmissionStatus status);

    List<AssignmentSubmission> findByEnrollmentUuid(UUID enrollmentUuid);

    List<AssignmentSubmission> findByAssignmentUuidAndPercentageGreaterThan(UUID assignmentUuid, BigDecimal percentage);

    Optional<AssignmentSubmission> findByEnrollmentUuidAndAssignmentUuid(UUID enrollmentUuid, UUID assignmentUuid);
}