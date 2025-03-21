package apps.sarafrika.elimika.assessment.factory;

import apps.sarafrika.elimika.assessment.dto.request.CreateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.model.AnswerOption;

public class AnswerOptionFactory {

    public static AnswerOption create(CreateAnswerOptionRequestDTO createAnswerOptionRequestDTO) {

        return AnswerOption.builder()
                .optionText(createAnswerOptionRequestDTO.optionText())
                .correct(createAnswerOptionRequestDTO.correct())
                .orderInQuestion(createAnswerOptionRequestDTO.orderInQuestion())
                .build();
    }

    public static void update(AnswerOption answerOption, UpdateAnswerOptionRequestDTO updateAnswerOptionRequestDTO) {

        answerOption.setOptionText(updateAnswerOptionRequestDTO.optionText());
        answerOption.setCorrect(updateAnswerOptionRequestDTO.correct());
        answerOption.setOrderInQuestion(updateAnswerOptionRequestDTO.orderInQuestion());
    }
}
