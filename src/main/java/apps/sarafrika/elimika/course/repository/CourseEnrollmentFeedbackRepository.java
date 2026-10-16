package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseEnrollmentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseEnrollmentFeedbackRepository extends JpaRepository<CourseEnrollmentFeedback, Long> {

    List<CourseEnrollmentFeedback> findByEnrollmentUuidOrderByCreatedDateDescIdDesc(UUID enrollmentUuid);

    Optional<CourseEnrollmentFeedback> findFirstByEnrollmentUuidOrderByCreatedDateDescIdDesc(UUID enrollmentUuid);
}
