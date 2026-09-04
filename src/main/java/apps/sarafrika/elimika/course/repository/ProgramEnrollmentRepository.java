package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long>, JpaSpecificationExecutor<ProgramEnrollment> {
    /**
     * Whether this student holds a programme enrolment on any programme owned by this course
     * creator. The programme-shaped twin of
     * {@code CourseEnrollmentRepository#existsForStudentAndCourseCreator}; a learner reached through
     * a programme is no less the creator's learner.
     */
    @Query("SELECT CASE WHEN COUNT(pe) > 0 THEN TRUE ELSE FALSE END " +
           "FROM ProgramEnrollment pe JOIN TrainingProgram tp ON pe.programUuid = tp.uuid " +
           "WHERE pe.studentUuid = :studentUuid AND tp.courseCreatorUuid = :courseCreatorUuid")
    boolean existsForStudentAndCourseCreator(@Param("studentUuid") UUID studentUuid,
                                             @Param("courseCreatorUuid") UUID courseCreatorUuid);

    Optional<ProgramEnrollment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    long countByProgramUuid(UUID programUuid);

    long countByProgramUuidAndStatus(UUID programUuid, EnrollmentStatus status);

    boolean existsByStudentUuidAndProgramUuidAndStatus(UUID studentUuid, UUID programUuid, EnrollmentStatus status);

    boolean existsByStudentUuidAndProgramUuidAndStatusIn(
            UUID studentUuid,
            UUID programUuid,
            Collection<EnrollmentStatus> statuses);

    boolean existsByUuid(UUID uuid);

    long countByStatus(EnrollmentStatus status);

    long countByEnrollmentDateAfter(LocalDateTime enrollmentDate);

    long countByStatusAndCompletionDateAfter(EnrollmentStatus status, LocalDateTime completionDate);

    Page<ProgramEnrollment> findByStudentUuid(UUID studentUuid, Pageable pageable);
}
