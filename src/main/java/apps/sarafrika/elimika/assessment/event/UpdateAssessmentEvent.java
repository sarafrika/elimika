package apps.sarafrika.elimika.assessment.event;

import apps.sarafrika.elimika.assessment.dto.request.UpdateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.persistence.Assessment;

public record UpdateAssessmentEvent(Assessment assessment, UpdateAssessmentRequestDTO updateAssessmentRequestDTO) {
}
