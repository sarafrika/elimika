package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.assessment.dto.request.CreateAssessmentRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAssessmentRequestDTO;

public class AssessmentFactory {

    public static Assessment create(final CreateAssessmentRequestDTO createAssessmentRequestDTO) {

        return Assessment.builder()
                .title(createAssessmentRequestDTO.title())
                .type(createAssessmentRequestDTO.type())
                .description(createAssessmentRequestDTO.description())
                .maximumScore(createAssessmentRequestDTO.maximumScore())
                .passingScore(createAssessmentRequestDTO.passingScore())
                .dueDate(createAssessmentRequestDTO.dueDate())
                .timeLimit(createAssessmentRequestDTO.timeLimit())
                .build();
    }

    public static void update(final Assessment assessment, final UpdateAssessmentRequestDTO updateAssessmentRequestDTO) {

        assessment.setTitle(updateAssessmentRequestDTO.title());
        assessment.setType(updateAssessmentRequestDTO.type());
        assessment.setDescription(updateAssessmentRequestDTO.description());
        assessment.setMaximumScore(updateAssessmentRequestDTO.maximumScore());
        assessment.setPassingScore(updateAssessmentRequestDTO.passingScore());
        assessment.setDueDate(updateAssessmentRequestDTO.dueDate());
        assessment.setTimeLimit(updateAssessmentRequestDTO.timeLimit());
    }
}
