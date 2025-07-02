package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AssessmentRubricService {
    AssessmentRubricDTO createAssessmentRubric(AssessmentRubricDTO assessmentRubricDTO);

    AssessmentRubricDTO getAssessmentRubricByUuid(UUID uuid);

    Page<AssessmentRubricDTO> getAllAssessmentRubrics(Pageable pageable);

    AssessmentRubricDTO updateAssessmentRubric(UUID uuid, AssessmentRubricDTO assessmentRubricDTO);

    void deleteAssessmentRubric(UUID uuid);

    Page<AssessmentRubricDTO> search(Map<String, String> searchParams, Pageable pageable);
}





