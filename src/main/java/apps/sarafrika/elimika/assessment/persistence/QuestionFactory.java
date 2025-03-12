package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.assessment.dto.request.CreateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateQuestionRequestDTO;

public class QuestionFactory {

    public static Question create(final CreateQuestionRequestDTO createQuestionRequestDTO) {

        return Question.builder()
                .description(createQuestionRequestDTO.description())
                .questionType(createQuestionRequestDTO.questionType())
                .pointValue(createQuestionRequestDTO.pointValue())
                .orderInAssessment(createQuestionRequestDTO.orderInAssessment())
                .build();
    }

    public static void update(final Question question, final UpdateQuestionRequestDTO updateQuestionRequestDTO) {

        question.setDescription(updateQuestionRequestDTO.description());
        question.setQuestionType(updateQuestionRequestDTO.questionType());
        question.setPointValue(updateQuestionRequestDTO.pointValue());
        question.setOrderInAssessment(updateQuestionRequestDTO.orderInAssessment());
    }
}
