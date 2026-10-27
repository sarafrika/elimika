package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LearnerSkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearnerSkillLevelRepository extends JpaRepository<LearnerSkillLevel, Long> {

    Optional<LearnerSkillLevel> findByStudentUuidAndProgramUuidAndSkillUuid(UUID studentUuid, UUID programUuid, UUID skillUuid);

    List<LearnerSkillLevel> findByStudentUuidAndProgramUuid(UUID studentUuid, UUID programUuid);

    List<LearnerSkillLevel> findByProgramUuidAndStudentUuidIn(UUID programUuid, Collection<UUID> studentUuids);
}
