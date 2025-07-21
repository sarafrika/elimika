package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long>,
        JpaSpecificationExecutor<CourseEnrollment> {
    Optional<CourseEnrollment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByStudentUuidAndCourseUuidAndStatus(UUID studentUuid, UUID courseUuid, EnrollmentStatus enrollmentStatus);

    boolean existsByCourseUuidAndStatusIn(UUID courseUuid, List<EnrollmentStatus> statuses);
}