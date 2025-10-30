package apps.sarafrika.elimika.shared.event.classes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event emitted whenever a class quiz schedule is created, updated, or deleted.
 */
public record ClassQuizScheduleChangedEventDTO(
        ClassAssessmentScheduleChangeType changeType,
        UUID scheduleUuid,
        UUID classDefinitionUuid,
        UUID lessonUuid,
        UUID quizUuid,
        String quizTitle,
        UUID classLessonPlanUuid,
        LocalDateTime visibleAt,
        LocalDateTime dueAt,
        String timezone,
        String releaseStrategy,
        Integer timeLimitOverride,
        Integer attemptLimitOverride,
        BigDecimal passingScoreOverride,
        UUID instructorUuid,
        String instructorEmail,
        String instructorName,
        String notes,
        String changedBy,
        LocalDateTime changedAt
) {
}
