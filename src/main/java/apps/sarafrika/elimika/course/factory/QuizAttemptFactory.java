package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.dto.QuizResponseDTO;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizAttemptFactory {

    // Convert QuizAttempt entity to QuizAttemptDTO
    public static QuizAttemptDTO toDTO(QuizAttempt quizAttempt) {
        if (quizAttempt == null) {
            return null;
        }
        return new QuizAttemptDTO(
                quizAttempt.getUuid(),
                quizAttempt.getEnrollmentUuid(),
                quizAttempt.getQuizUuid(),
                quizAttempt.getAttemptNumber(),
                quizAttempt.getStartedAt(),
                quizAttempt.getSubmittedAt(),
                quizAttempt.getTimeTakenMinutes(),
                quizAttempt.getScore(),
                quizAttempt.getMaxScore(),
                quizAttempt.getPercentage(),
                quizAttempt.getIsPassed(),
                quizAttempt.getStatus(),
                quizAttempt.getCreatedDate(),
                quizAttempt.getCreatedBy(),
                quizAttempt.getLastModifiedDate(),
                quizAttempt.getLastModifiedBy()
        );
    }

    // Convert QuizAttemptDTO to QuizAttempt entity
    public static QuizAttempt toEntity(QuizAttemptDTO dto) {
        if (dto == null) {
            return null;
        }
        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUuid(dto.uuid());
        quizAttempt.setEnrollmentUuid(dto.enrollmentUuid());
        quizAttempt.setQuizUuid(dto.quizUuid());
        quizAttempt.setAttemptNumber(dto.attemptNumber());
        quizAttempt.setStartedAt(dto.startedAt());
        quizAttempt.setSubmittedAt(dto.submittedAt());
        quizAttempt.setTimeTakenMinutes(dto.timeTakenMinutes());
        quizAttempt.setScore(dto.score());
        quizAttempt.setMaxScore(dto.maxScore());
        quizAttempt.setPercentage(dto.percentage());
        quizAttempt.setIsPassed(dto.isPassed());
        quizAttempt.setStatus(dto.status());
        quizAttempt.setCreatedDate(dto.createdDate());
        quizAttempt.setCreatedBy(dto.createdBy());
        quizAttempt.setLastModifiedDate(dto.updatedDate());
        quizAttempt.setLastModifiedBy(dto.updatedBy());
        return quizAttempt;
    }
}
