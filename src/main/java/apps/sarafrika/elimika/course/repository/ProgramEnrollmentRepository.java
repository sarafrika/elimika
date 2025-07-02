package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramEnrollmentRepository extends JpaRepository<ProgramEnrollment, Long>, JpaSpecificationExecutor<ProgramEnrollment> {
    Optional<ProgramEnrollment> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}