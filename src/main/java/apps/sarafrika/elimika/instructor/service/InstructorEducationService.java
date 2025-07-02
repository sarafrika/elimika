package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorEducationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InstructorEducationService {
    InstructorEducationDTO createInstructorEducation(InstructorEducationDTO instructorEducationDTO);
    InstructorEducationDTO getInstructorEducationByUuid(UUID uuid);
    Page<InstructorEducationDTO> getAllInstructorEducation(Pageable pageable);
    InstructorEducationDTO updateInstructorEducation(UUID uuid, InstructorEducationDTO instructorEducationDTO);
    void deleteInstructorEducation(UUID uuid);
    Page<InstructorEducationDTO> search(Map<String, String> searchParams, Pageable pageable);

    // Additional methods specific to InstructorEducation
    List<InstructorEducationDTO> getEducationByInstructorUuid(UUID instructorUuid);
}