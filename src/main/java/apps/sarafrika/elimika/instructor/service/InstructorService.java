package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface InstructorService {
    InstructorDTO createInstructor(InstructorDTO instructorDTO);
    InstructorDTO getInstructorByUuid(UUID uuid);
    Page<InstructorDTO> getAllInstructors(Pageable pageable);
    InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO);
    void deleteInstructor(UUID uuid);
    Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable);
}
