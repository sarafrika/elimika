package apps.sarafrika.elimika.assessment.dto.request;

import java.util.List;

public record CreateQuestionRequestDTO(
        String description,
        String questionType,
        int pointValue,
        int orderInAssessment,
        List<CreateAnswerOptionRequestDTO> answerOptions
) {
}
