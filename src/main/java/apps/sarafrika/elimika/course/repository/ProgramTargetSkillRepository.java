package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramTargetSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramTargetSkillRepository extends JpaRepository<ProgramTargetSkill, Long> {

    List<ProgramTargetSkill> findByProgramUuidOrderByDisplayOrderAsc(UUID programUuid);

    void deleteByProgramUuid(UUID programUuid);
}
