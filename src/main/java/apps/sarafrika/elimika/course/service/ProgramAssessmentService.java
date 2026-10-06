package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramAssessmentDTO;

import java.util.List;
import java.util.UUID;

public interface ProgramAssessmentService {

    ProgramAssessmentDTO create(UUID programUuid, ProgramAssessmentDTO dto);

    List<ProgramAssessmentDTO> list(UUID programUuid);

    ProgramAssessmentDTO update(UUID programUuid, UUID assessmentUuid, ProgramAssessmentDTO dto);

    void delete(UUID programUuid, UUID assessmentUuid);

    /** Active component weights must total exactly 100% before the program is published. */
    void enforceFullWeight(UUID programUuid);
}
