package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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

}