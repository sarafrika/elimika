package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.QuizQuestionDTO;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizQuestionFactory {

    // Convert QuizQuestion entity to QuizQuestionDTO
    public static QuizQuestionDTO toDTO(QuizQuestion quizQuestion) {
        if (quizQuestion == null) {
            return null;
        }
        return new QuizQuestionDTO(
                quizQuestion.getUuid(),
                quizQuestion.getQuizUuid(),
                quizQuestion.getQuestionText(),
                quizQuestion.getQuestionType(),
                quizQuestion.getPoints(),
                quizQuestion.getDisplayOrder(),
                quizQuestion.getCreatedDate(),
                quizQuestion.getCreatedBy(),
                quizQuestion.getLastModifiedDate(),
                quizQuestion.getLastModifiedBy()
        );
    }

    // Convert QuizQuestionDTO to QuizQuestion entity
    public static QuizQuestion toEntity(QuizQuestionDTO dto) {
        if (dto == null) {
            return null;
        }
        QuizQuestion quizQuestion = new QuizQuestion();
        quizQuestion.setUuid(dto.uuid());
        quizQuestion.setQuizUuid(dto.quizUuid());
        quizQuestion.setQuestionText(dto.questionText());
        quizQuestion.setQuestionType(dto.questionType());
        quizQuestion.setPoints(dto.points());
        quizQuestion.setDisplayOrder(dto.displayOrder());
        quizQuestion.setCreatedDate(dto.createdDate());
        quizQuestion.setCreatedBy(dto.createdBy());
        quizQuestion.setLastModifiedDate(dto.updatedDate());
        quizQuestion.setLastModifiedBy(dto.updatedBy());
        return quizQuestion;
    }
}