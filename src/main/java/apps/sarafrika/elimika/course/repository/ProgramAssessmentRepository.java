package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramAssessmentRepository extends JpaRepository<ProgramAssessment, Long> {

    List<ProgramAssessment> findByProgramUuidOrderByCreatedDateAsc(UUID programUuid);

    Optional<ProgramAssessment> findByUuidAndProgramUuid(UUID uuid, UUID programUuid);

    Optional<ProgramAssessment> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);
}
