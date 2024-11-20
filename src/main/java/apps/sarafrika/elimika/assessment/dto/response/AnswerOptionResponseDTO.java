package apps.sarafrika.elimika.assessment.dto.response;

import apps.sarafrika.elimika.assessment.persistence.AnswerOption;

public record AnswerOptionResponseDTO(
        Long id,
        String optionText,
        boolean correct,
        int orderInQuestion
) {

    public static AnswerOptionResponseDTO from(AnswerOption answerOption) {

        return new AnswerOptionResponseDTO(
                answerOption.getId(),
                answerOption.getOptionText(),
                answerOption.isCorrect(),
                answerOption.getOrderInQuestion()
        );
    }
}
