package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramSkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProgramSkillMappingRepository extends JpaRepository<ProgramSkillMapping, Long> {

    List<ProgramSkillMapping> findByProgramUuidAndActiveTrue(UUID programUuid);

    List<ProgramSkillMapping> findByCourseAssessmentUuidAndActiveTrue(UUID courseAssessmentUuid);

    void deleteByProgramUuidAndSkillUuid(UUID programUuid, UUID skillUuid);

    void deleteByProgramUuidAndSkillUuidNotIn(UUID programUuid, Collection<UUID> skillUuids);

    void deleteByProgramUuid(UUID programUuid);
}
