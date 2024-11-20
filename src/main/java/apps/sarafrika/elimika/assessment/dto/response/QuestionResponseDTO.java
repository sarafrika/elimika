package apps.sarafrika.elimika.assessment.dto.response;

import apps.sarafrika.elimika.assessment.persistence.Question;

import java.util.List;

public record QuestionResponseDTO(
        Long id,
        String description,
        String questionType,
        int pointValue,
        int orderInAssessment,
        List<AnswerOptionResponseDTO> answerOptions
) {

    public static QuestionResponseDTO from(Question question, List<AnswerOptionResponseDTO> answerOptions) {

        return new QuestionResponseDTO(
                question.getId(),
                question.getDescription(),
                question.getQuestionType(),
                question.getPointValue(),
                question.getOrderInAssessment(),
                answerOptions
        );
    }
}
