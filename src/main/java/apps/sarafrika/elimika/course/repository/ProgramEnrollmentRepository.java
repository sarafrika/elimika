package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long>, JpaSpecificationExecutor<ProgramEnrollment> {
    Optional<ProgramEnrollment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    long countByProgramUuid(UUID programUuid);

    long countByProgramUuidAndStatus(UUID programUuid, EnrollmentStatus status);

    boolean existsByStudentUuidAndProgramUuidAndStatus(UUID studentUuid, UUID programUuid, EnrollmentStatus status);

    boolean existsByUuid(UUID uuid);

    long countByStatus(EnrollmentStatus status);

    long countByEnrollmentDateAfter(LocalDateTime enrollmentDate);

    long countByStatusAndCompletionDateAfter(EnrollmentStatus status, LocalDateTime completionDate);

    Page<ProgramEnrollment> findByStudentUuid(UUID studentUuid, Pageable pageable);
}
