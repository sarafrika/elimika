package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationService;
import apps.sarafrika.elimika.notifications.api.events.ClassScheduleUpdatedNotificationEvent;
import apps.sarafrika.elimika.shared.event.classes.ClassAssessmentScheduleChangeType;
import apps.sarafrika.elimika.shared.event.classes.ClassAssignmentScheduleChangedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassQuizScheduleChangedEventDTO;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for class assessment schedule events and notifies instructors about changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleNotificationListener {

    private final NotificationService notificationService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;

    @ApplicationModuleListener
    @Async
    public void handleAssignmentScheduleChanged(ClassAssignmentScheduleChangedEventDTO event) {
        sendInstructorNotification(
                event.instructorUuid(),
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
        if (instructorUuid == null) {
            log.warn("Skipping schedule notification: instructor UUID missing for schedule {}", scheduleUuid);
            return;
        }

        Optional<UUID> instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid);
        if (instructorUserUuid.isEmpty()) {
            log.warn("Skipping schedule notification: no user mapping for instructor {}", instructorUuid);
            return;
        }

        UUID userUuid = instructorUserUuid.get();
        String email = userLookupService.getUserEmail(userUuid).orElse(null);
        if (email == null) {
            log.warn("Skipping schedule notification: no email for instructor user {}", userUuid);
            return;
        }

        String recipientName = userLookupService.getUserFullName(userUuid).orElse("Instructor");

        ClassScheduleUpdatedNotificationEvent notificationEvent = new ClassScheduleUpdatedNotificationEvent(
                UUID.randomUUID(),
                userUuid,
                email,
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
