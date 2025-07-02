package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorExperienceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface InstructorExperienceService {
    InstructorExperienceDTO createInstructorExperience(InstructorExperienceDTO instructorExperienceDTO);
    InstructorExperienceDTO getInstructorExperienceByUuid(UUID uuid);
    Page<InstructorExperienceDTO> getAllInstructorExperience(Pageable pageable);
    InstructorExperienceDTO updateInstructorExperience(UUID uuid, InstructorExperienceDTO instructorExperienceDTO);
    void deleteInstructorExperience(UUID uuid);
    Page<InstructorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable);
}