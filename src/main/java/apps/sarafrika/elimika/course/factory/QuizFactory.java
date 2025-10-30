package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.QuizDTO;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.util.enums.QuizScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuizFactory {

    // Convert Quiz entity to QuizDTO
    public static QuizDTO toDTO(Quiz quiz) {
        if (quiz == null) {
            return null;
        }
        return new QuizDTO(
                quiz.getUuid(),
                quiz.getLessonUuid(),
                quiz.getScope(),
                quiz.getClassDefinitionUuid(),
                quiz.getSourceQuizUuid(),
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
        quiz.setScope(dto.scope() != null ? dto.scope() : QuizScope.COURSE_TEMPLATE);
        quiz.setClassDefinitionUuid(dto.classDefinitionUuid());
        quiz.setSourceQuizUuid(dto.sourceQuizUuid());
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
