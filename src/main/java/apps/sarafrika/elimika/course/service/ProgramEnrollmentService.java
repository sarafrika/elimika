package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramEnrollmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ProgramEnrollmentService {
    ProgramEnrollmentDTO createProgramEnrollment(ProgramEnrollmentDTO programEnrollmentDTO);

    ProgramEnrollmentDTO getProgramEnrollmentByUuid(UUID uuid);

    Page<ProgramEnrollmentDTO> getAllProgramEnrollments(Pageable pageable);

    ProgramEnrollmentDTO updateProgramEnrollment(UUID uuid, ProgramEnrollmentDTO programEnrollmentDTO);

    void deleteProgramEnrollment(UUID uuid);

    Page<ProgramEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable);
}