package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.QuizDTO;
import apps.sarafrika.elimika.course.model.Quiz;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizFactory {

    // Convert Quiz entity to QuizDTO
    public static QuizDTO toDTO(Quiz quiz) {
        if (quiz == null) {
            return null;
        }
        return new QuizDTO(
                quiz.getUuid(),
                quiz.getLessonUuid(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getInstructions(),
                quiz.getTimeLimitMinutes(),
                quiz.getAttemptsAllowed(),
                quiz.getPassingScore(),
                quiz.getRubricUuid(),
                quiz.getStatus(),
                quiz.getActive(),
                quiz.getCreatedDate(),
                quiz.getCreatedBy(),
                quiz.getLastModifiedDate(),
                quiz.getLastModifiedBy()
        );
    }

    // Convert QuizDTO to Quiz entity
    public static Quiz toEntity(QuizDTO dto) {
        if (dto == null) {
            return null;
        }
        Quiz quiz = new Quiz();
        quiz.setUuid(dto.uuid());
        quiz.setLessonUuid(dto.lessonUuid());
        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setInstructions(dto.instructions());
        quiz.setTimeLimitMinutes(dto.timeLimitMinutes());
        quiz.setAttemptsAllowed(dto.attemptsAllowed());
        quiz.setPassingScore(dto.passingScore());
        quiz.setRubricUuid(dto.rubricUuid());
        quiz.setStatus(dto.status());
        quiz.setActive(dto.active());
        quiz.setCreatedDate(dto.createdDate());
        quiz.setCreatedBy(dto.createdBy());
        quiz.setLastModifiedDate(dto.updatedDate());
        quiz.setLastModifiedBy(dto.updatedBy());
        return quiz;
    }
}