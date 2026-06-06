package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class UpcomingClassReminderScheduler {

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final StudentLookupService studentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 * * * * *")
    void sendUpcomingClassReminders() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<ScheduledInstance> upcomingInstances = scheduledInstanceRepository
                .findScheduledInstancesStartingBetween(now, now.plusHours(24));

        for (ScheduledInstance instance : upcomingInstances) {
            publishReminderIfDue(instance, now);
        }
    }

    private void publishReminderIfDue(ScheduledInstance instance, LocalDateTime now) {
        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = instance.getClassDefinitionUuid() == null
                ? null
                : classDefinitionLookupService.findByUuid(instance.getClassDefinitionUuid()).orElse(null);
        Integer reminderMinutes = snapshot == null ? null : snapshot.classReminderMinutes();
        if (reminderMinutes == null || reminderMinutes < 0 || instance.getStartTime() == null) {
            return;
        }

        LocalDateTime reminderAt = instance.getStartTime().minusMinutes(reminderMinutes);
        if (now.isBefore(reminderAt) || !now.isBefore(instance.getStartTime())) {
            return;
        }

        String classTitle = resolveClassTitle(instance, snapshot);
        publishInstructorReminder(instance, classTitle, reminderMinutes);
        publishStudentReminders(instance, classTitle, reminderMinutes);
    }

    private void publishInstructorReminder(ScheduledInstance instance, String classTitle, int reminderMinutes) {
        if (instance.getInstructorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = instructorLookupService.getInstructorUserUuid(instance.getInstructorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "UPCOMING_CLASS_REMINDER",
                "POPUP",
                "Class starts soon",
                classTitle + " starts in " + reminderMinutes + " minutes.",
                resolveClassActionUrl(instance),
                reminderMetadata(instance, reminderMinutes, "instructor"),
                "upcoming-class-reminder:instructor:" + instance.getUuid() + ":" + reminderMinutes
        ));
    }

    private void publishStudentReminders(ScheduledInstance instance, String classTitle, int reminderMinutes) {
        List<Enrollment> enrollments = enrollmentRepository.findByScheduledInstanceUuidAndStatus(
                instance.getUuid(),
                EnrollmentStatus.ENROLLED
        );

        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentUuid() == null) {
                continue;
            }
            UUID recipientUserUuid = studentLookupService.getStudentUserUuid(enrollment.getStudentUuid())
                    .orElse(null);
            if (recipientUserUuid == null) {
                continue;
            }

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipientUserUuid,
                    "UPCOMING_CLASS_REMINDER",
                    "POPUP",
                    "Class starts soon",
                    classTitle + " starts in " + reminderMinutes + " minutes.",
                    resolveClassActionUrl(instance),
                    reminderMetadata(instance, reminderMinutes, "student"),
                    "upcoming-class-reminder:student:" + instance.getUuid() + ":" + enrollment.getStudentUuid() + ":" + reminderMinutes
            ));
        }
    }

    private Map<String, Object> reminderMetadata(ScheduledInstance instance, int reminderMinutes, String recipientRole) {
        return Map.of(
                "scheduled_instance_uuid", instance.getUuid() == null ? "" : instance.getUuid().toString(),
                "class_definition_uuid", instance.getClassDefinitionUuid() == null ? "" : instance.getClassDefinitionUuid().toString(),
                "instructor_uuid", instance.getInstructorUuid() == null ? "" : instance.getInstructorUuid().toString(),
                "start_time", instance.getStartTime() == null ? "" : instance.getStartTime().toString(),
                "reminder_minutes", reminderMinutes,
                "recipient_role", recipientRole
        );
    }

    private String resolveClassTitle(ScheduledInstance instance, ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot != null && snapshot.title() != null && !snapshot.title().isBlank()) {
            return snapshot.title();
        }
        if (instance.getTitle() != null && !instance.getTitle().isBlank()) {
            return instance.getTitle();
        }
        return "Your class";
    }

    private String resolveClassActionUrl(ScheduledInstance instance) {
        if (instance.getClassDefinitionUuid() != null) {
            return "/dashboard/classes/" + instance.getClassDefinitionUuid();
        }
        return "/dashboard/classes/schedule/" + instance.getUuid();
    }
}
