package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstructorExperienceRepository extends JpaRepository<InstructorExperience, Long>,
    JpaSpecificationExecutor<InstructorExperience>{

    Optional<InstructorExperience> findByUuid(UUID uuid);

    /** Batch load for search documents, in insertion order. */
    List<InstructorExperience> findByInstructorUuidInOrderByIdAsc(Collection<UUID> instructorUuids);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

}
