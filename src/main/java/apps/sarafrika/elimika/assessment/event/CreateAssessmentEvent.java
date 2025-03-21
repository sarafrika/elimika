package apps.sarafrika.elimika.assessment.event;

import apps.sarafrika.elimika.assessment.dto.request.CreateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.model.Assessment;

public record CreateAssessmentEvent(Assessment assessment, CreateAssessmentRequestDTO createAssessmentRequestDTO) {
}
