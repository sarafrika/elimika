package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramSkillMappingDTO;
import apps.sarafrika.elimika.course.dto.ProgramTargetSkillDTO;

import java.util.List;
import java.util.UUID;

/** A programme's target skills and the graded rubric criteria or whole scores that show each one. */
public interface ProgramSkillService {

    List<ProgramTargetSkillDTO> getSkills(UUID programUuid);

    /** Replaces the target skills in order; a removed skill loses its links. */
    List<ProgramTargetSkillDTO> replaceTargetSkills(UUID programUuid, List<UUID> skillUuids);

    /** Replaces one target skill's links; shares total 100 per assessment. */
    ProgramTargetSkillDTO replaceMappings(UUID programUuid, UUID skillUuid, List<ProgramSkillMappingDTO> mappings);

    /** Throws unless every target skill has at least one link. */
    void enforceSkillsMapped(UUID programUuid);
}
