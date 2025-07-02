package apps.sarafrika.elimika.course.factory;


import apps.sarafrika.elimika.course.dto.QuizResponseDTO;
import apps.sarafrika.elimika.course.model.QuizResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizResponseFactory {

    // Convert QuizResponse entity to QuizResponseDTO
    public static QuizResponseDTO toDTO(QuizResponse quizResponse) {
        if (quizResponse == null) {
            return null;
        }
        return new QuizResponseDTO(
                quizResponse.getUuid(),
                quizResponse.getAttemptUuid(),
                quizResponse.getQuestionUuid(),
                quizResponse.getSelectedOptionUuid(),
                quizResponse.getTextResponse(),
                quizResponse.getPointsEarned(),
                quizResponse.getIsCorrect(),
                quizResponse.getCreatedDate(),
                quizResponse.getCreatedBy(),
                quizResponse.getLastModifiedDate(),
                quizResponse.getLastModifiedBy()
        );
    }

    // Convert QuizResponseDTO to QuizResponse entity
    public static QuizResponse toEntity(QuizResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        QuizResponse quizResponse = new QuizResponse();
        quizResponse.setUuid(dto.uuid());
        quizResponse.setAttemptUuid(dto.attemptUuid());
        quizResponse.setQuestionUuid(dto.questionUuid());
        quizResponse.setSelectedOptionUuid(dto.selectedOptionUuid());
        quizResponse.setTextResponse(dto.textResponse());
        quizResponse.setPointsEarned(dto.pointsEarned());
        quizResponse.setIsCorrect(dto.isCorrect());
        quizResponse.setCreatedDate(dto.createdDate());
        quizResponse.setCreatedBy(dto.createdBy());
        quizResponse.setLastModifiedDate(dto.updatedDate());
        quizResponse.setLastModifiedBy(dto.updatedBy());
        return quizResponse;
    }
}