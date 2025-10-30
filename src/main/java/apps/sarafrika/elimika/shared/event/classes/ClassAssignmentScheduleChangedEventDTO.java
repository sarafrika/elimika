package apps.sarafrika.elimika.shared.event.classes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event emitted whenever a class assignment schedule is created, updated, or deleted.
 */
public record ClassAssignmentScheduleChangedEventDTO(
        ClassAssessmentScheduleChangeType changeType,
        UUID scheduleUuid,
        UUID classDefinitionUuid,
        UUID lessonUuid,
        UUID assignmentUuid,
        String assignmentTitle,
        UUID classLessonPlanUuid,
        LocalDateTime visibleAt,
        LocalDateTime dueAt,
        LocalDateTime gradingDueAt,
        String timezone,
        String releaseStrategy,
        Integer maxAttempts,
        UUID instructorUuid,
        String instructorEmail,
        String instructorName,
        String notes,
        String changedBy,
        LocalDateTime changedAt
) {
}
