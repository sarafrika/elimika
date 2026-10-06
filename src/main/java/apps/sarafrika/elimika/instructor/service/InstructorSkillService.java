package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface InstructorSkillService {
    InstructorSkillDTO createInstructorSkill(InstructorSkillDTO instructorSkillDTO);
    InstructorSkillDTO getInstructorSkillByUuid(UUID uuid);
    InstructorSkillDTO updateInstructorSkill(UUID uuid, InstructorSkillDTO instructorSkillDTO);
    void deleteInstructorSkill(UUID uuid);
    Page<InstructorSkillDTO> search(Map<String, String> searchParams, Pageable pageable);
}
