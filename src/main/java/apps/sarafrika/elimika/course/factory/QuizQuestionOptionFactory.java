package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.QuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizQuestionOptionFactory {

    // Convert QuizQuestionOption entity to QuizQuestionOptionDTO
    public static QuizQuestionOptionDTO toDTO(QuizQuestionOption quizQuestionOption) {
        if (quizQuestionOption == null) {
            return null;
        }
        return new QuizQuestionOptionDTO(
                quizQuestionOption.getUuid(),
                quizQuestionOption.getQuestionUuid(),
                quizQuestionOption.getOptionText(),
                quizQuestionOption.getIsCorrect(),
                quizQuestionOption.getDisplayOrder(),
                quizQuestionOption.getCreatedDate(),
                quizQuestionOption.getCreatedBy(),
                quizQuestionOption.getLastModifiedDate(),
                quizQuestionOption.getLastModifiedBy()
        );
    }

    // Convert QuizQuestionOptionDTO to QuizQuestionOption entity
    public static QuizQuestionOption toEntity(QuizQuestionOptionDTO dto) {
        if (dto == null) {
            return null;
        }
        QuizQuestionOption quizQuestionOption = new QuizQuestionOption();
        quizQuestionOption.setUuid(dto.uuid());
        quizQuestionOption.setQuestionUuid(dto.questionUuid());
        quizQuestionOption.setOptionText(dto.optionText());
        quizQuestionOption.setIsCorrect(dto.isCorrect());
        quizQuestionOption.setDisplayOrder(dto.displayOrder());
        quizQuestionOption.setCreatedDate(dto.createdDate());
        quizQuestionOption.setCreatedBy(dto.createdBy());
        quizQuestionOption.setLastModifiedDate(dto.updatedDate());
        quizQuestionOption.setLastModifiedBy(dto.updatedBy());
        return quizQuestionOption;
    }
}