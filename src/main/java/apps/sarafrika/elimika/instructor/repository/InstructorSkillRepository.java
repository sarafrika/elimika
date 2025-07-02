package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface InstructorSkillRepository extends JpaRepository<InstructorSkill, Long>,
    JpaSpecificationExecutor<InstructorSkill>{

    Optional<InstructorSkill> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

}
