package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long>,
        JpaSpecificationExecutor<CourseEnrollment> {
    /**
     * Whether this student holds a course enrolment on any course owned by this course creator.
     * <p>
     * Backs the contact-visibility check on the course creator's enrolled-learner page. Enrolment
     * status is not filtered: a creator chasing a learner who dropped needs to reach them exactly
     * as much as one chasing an active learner.
     */
    @Query("SELECT CASE WHEN COUNT(ce) > 0 THEN TRUE ELSE FALSE END " +
           "FROM CourseEnrollment ce JOIN Course c ON ce.courseUuid = c.uuid " +
           "WHERE ce.studentUuid = :studentUuid AND c.courseCreatorUuid = :courseCreatorUuid")
    boolean existsForStudentAndCourseCreator(@Param("studentUuid") UUID studentUuid,
                                             @Param("courseCreatorUuid") UUID courseCreatorUuid);

    Optional<CourseEnrollment> findByUuid(UUID uuid);

    Optional<CourseEnrollment> findByUuidAndCourseUuid(UUID uuid, UUID courseUuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByStudentUuidAndCourseUuid(UUID studentUuid, UUID courseUuid);

    boolean existsByStudentUuidAndCourseUuidAndStatus(UUID studentUuid, UUID courseUuid, EnrollmentStatus enrollmentStatus);

    Optional<CourseEnrollment> findByStudentUuidAndCourseUuid(UUID studentUuid, UUID courseUuid);

    List<CourseEnrollment> findByCourseUuid(UUID courseUuid);

    boolean existsByCourseUuidAndStatusIn(UUID courseUuid, List<EnrollmentStatus> statuses);

    long countByStatus(EnrollmentStatus status);

    long countByEnrollmentDateAfter(LocalDateTime enrolledAfter);

    long countByStatusAndCompletionDateAfter(EnrollmentStatus status, LocalDateTime completedAfter);

    @Query("SELECT COALESCE(AVG(ce.progressPercentage), 0) FROM CourseEnrollment ce WHERE ce.progressPercentage IS NOT NULL")
    BigDecimal calculateAverageProgressPercentage();

    Page<CourseEnrollment> findByStudentUuid(UUID studentUuid, Pageable pageable);

    @Query("SELECT ce.uuid FROM CourseEnrollment ce WHERE ce.studentUuid = :studentUuid")
    List<UUID> findEnrollmentUuidsByStudentUuid(@Param("studentUuid") UUID studentUuid);

    /**
     * Courses the student may still enter. Loading the whole set in one query lets callers answer
     * "is this learner in course X?" by set membership instead of a lookup per course.
     */
    @Query("""
            SELECT ce.courseUuid FROM CourseEnrollment ce
            WHERE ce.studentUuid = :studentUuid AND ce.status IN :statuses
            """)
    List<UUID> findCourseUuidsByStudentUuidAndStatusIn(@Param("studentUuid") UUID studentUuid,
                                                       @Param("statuses") Collection<EnrollmentStatus> statuses);
}
