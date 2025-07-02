package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramRequirementRepository extends JpaRepository<ProgramRequirement, Long>, JpaSpecificationExecutor<ProgramRequirement> {
    Optional<ProgramRequirement> findByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
}