package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationService;
import apps.sarafrika.elimika.notifications.api.events.ClassScheduleUpdatedNotificationEvent;
import apps.sarafrika.elimika.shared.event.classes.ClassAssessmentScheduleChangeType;
import apps.sarafrika.elimika.shared.event.classes.ClassAssignmentScheduleChangedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassQuizScheduleChangedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listens for class assessment schedule events and notifies instructors about changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleNotificationListener {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    @Async
    public void handleAssignmentScheduleChanged(ClassAssignmentScheduleChangedEventDTO event) {
        sendInstructorNotification(
                event.instructorUuid(),
                event.instructorEmail(),
                event.instructorName(),
                "Assignment",
                event.assignmentTitle(),
                event.changeType(),
                event.scheduleUuid(),
                event.classDefinitionUuid(),
                event.lessonUuid(),
                event.assignmentUuid(),
                event.visibleAt(),
                event.dueAt(),
                event.releaseStrategy(),
                event.timezone(),
                event.notes(),
                event.changedBy(),
                event.changedAt()
        );
    }

    @ApplicationModuleListener
    @Async
    public void handleQuizScheduleChanged(ClassQuizScheduleChangedEventDTO event) {
        sendInstructorNotification(
                event.instructorUuid(),
                event.instructorEmail(),
                event.instructorName(),
                "Quiz",
                event.quizTitle(),
                event.changeType(),
                event.scheduleUuid(),
                event.classDefinitionUuid(),
                event.lessonUuid(),
                event.quizUuid(),
                event.visibleAt(),
                event.dueAt(),
                event.releaseStrategy(),
                event.timezone(),
                event.notes(),
                event.changedBy(),
                event.changedAt()
        );
    }

    private void sendInstructorNotification(
            UUID instructorUuid,
            String instructorEmail,
            String instructorName,
            String assessmentType,
            String assessmentTitle,
            ClassAssessmentScheduleChangeType changeType,
            UUID scheduleUuid,
            UUID classDefinitionUuid,
            UUID lessonUuid,
            UUID assessmentUuid,
            LocalDateTime visibleAt,
            LocalDateTime dueAt,
            String releaseStrategy,
            String timezone,
            String notes,
            String changedBy,
            LocalDateTime changedAt
    ) {
        if (instructorEmail == null) {
            log.warn("Skipping schedule notification: instructor email missing for schedule {}", scheduleUuid);
            return;
        }

        String recipientName = instructorName != null ? instructorName : "Instructor";

        ClassScheduleUpdatedNotificationEvent notificationEvent = new ClassScheduleUpdatedNotificationEvent(
                UUID.randomUUID(),
                instructorUuid,
                instructorEmail,
                recipientName,
                classDefinitionUuid,
                scheduleUuid,
                lessonUuid,
                assessmentUuid,
                assessmentTitle,
                assessmentType,
                changeType,
                visibleAt,
                dueAt,
                releaseStrategy,
                timezone,
                notes,
                changedBy,
                changedAt,
                LocalDateTime.now()
        );

        notificationService.sendNotification(notificationEvent)
                .exceptionally(throwable -> {
                    log.error("Failed to dispatch class schedule notification {}: {}", scheduleUuid, throwable.getMessage());
                    return null;
                });
    }
}
