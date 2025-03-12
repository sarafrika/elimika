package apps.sarafrika.elimika.assessment.dto.request;

import java.util.List;

public record UpdateQuestionRequestDTO(
        String description,
        String questionType,
        int pointValue,
        int orderInAssessment,
        List<UpdateAnswerOptionRequestDTO> answerOptions
) {
}
