package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentGradeRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseAssessmentGradeRevisionRepository extends JpaRepository<CourseAssessmentGradeRevision, Long> {

    List<CourseAssessmentGradeRevision> findByLineItemUuidAndEnrollmentUuidOrderByRevisionNumberAsc(
            UUID lineItemUuid, UUID enrollmentUuid);

    @Query("SELECT COALESCE(MAX(r.revisionNumber), 0) FROM CourseAssessmentGradeRevision r "
            + "WHERE r.lineItemUuid = :lineItemUuid AND r.enrollmentUuid = :enrollmentUuid")
    int findLatestRevisionNumber(@Param("lineItemUuid") UUID lineItemUuid, @Param("enrollmentUuid") UUID enrollmentUuid);
}
