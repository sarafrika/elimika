package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    long countByStatus(EnrollmentStatus status);

    long countByEnrollmentDateAfter(LocalDateTime enrolledAfter);

    long countByStatusAndCompletionDateAfter(EnrollmentStatus status, LocalDateTime completedAfter);

    @Query("SELECT COALESCE(AVG(ce.progressPercentage), 0) FROM CourseEnrollment ce WHERE ce.progressPercentage IS NOT NULL")
    BigDecimal calculateAverageProgressPercentage();

    Page<CourseEnrollment> findByStudentUuid(UUID studentUuid, Pageable pageable);
}
