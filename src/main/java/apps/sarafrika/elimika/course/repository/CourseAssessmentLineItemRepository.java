package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssessmentLineItemRepository extends JpaRepository<CourseAssessmentLineItem, Long> {

    Optional<CourseAssessmentLineItem> findByUuid(UUID uuid);

    Optional<CourseAssessmentLineItem> findByUuidAndCourseAssessmentUuid(UUID uuid, UUID courseAssessmentUuid);

    List<CourseAssessmentLineItem> findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(UUID courseAssessmentUuid);

    List<CourseAssessmentLineItem> findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(Collection<UUID> courseAssessmentUuids);

    Optional<CourseAssessmentLineItem> findByCourseAssessmentUuidAndScheduledInstanceUuid(UUID courseAssessmentUuid, UUID scheduledInstanceUuid);

    Optional<CourseAssessmentLineItem> findByAssignmentUuid(UUID assignmentUuid);

    Optional<CourseAssessmentLineItem> findByQuizUuid(UUID quizUuid);

    void deleteByUuid(UUID uuid);
}
