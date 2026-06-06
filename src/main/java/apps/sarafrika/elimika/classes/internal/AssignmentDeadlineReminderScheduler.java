package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
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
class AssignmentDeadlineReminderScheduler {

    private final ClassAssignmentScheduleRepository assignmentScheduleRepository;
    private final TimetableService timetableService;
    private final StudentLookupService studentLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 * * * * *")
    void sendAssignmentDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        publishOneDayReminders(now);
        publishDueNowReminders(now);
    }

    private void publishOneDayReminders(LocalDateTime now) {
        List<ClassAssignmentSchedule> schedules = assignmentScheduleRepository.findByDueAtBetween(
                now,
                now.plusDays(1)
        );
        for (ClassAssignmentSchedule schedule : schedules) {
            if (schedule.getDueAt() == null || now.isBefore(schedule.getDueAt().minusDays(1))) {
                continue;
            }
            publishStudentDeadlineReminders(schedule, "one_day", "Assignment due tomorrow");
        }
    }

    private void publishDueNowReminders(LocalDateTime now) {
        List<ClassAssignmentSchedule> schedules = assignmentScheduleRepository.findByDueAtBetween(
                now.minusMinutes(1),
                now.plusMinutes(1)
        );
        for (ClassAssignmentSchedule schedule : schedules) {
            publishStudentDeadlineReminders(schedule, "due_now", "Assignment due now");
        }
    }

    private void publishStudentDeadlineReminders(ClassAssignmentSchedule schedule, String phase, String title) {
        if (schedule.getClassDefinitionUuid() == null) {
            return;
        }

        List<UUID> studentUuids = timetableService.getActiveStudentUuidsForClass(schedule.getClassDefinitionUuid());
        for (UUID studentUuid : studentUuids) {
            UUID recipientUserUuid = studentLookupService.getStudentUserUuid(studentUuid).orElse(null);
            if (recipientUserUuid == null) {
                continue;
            }

            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    recipientUserUuid,
                    "ASSIGNMENT_DEADLINE_REMINDER",
                    "POPUP",
                    title,
                    buildBody(phase),
                    "/dashboard/classes/" + schedule.getClassDefinitionUuid() + "/assignments/" + schedule.getAssignmentUuid(),
                    deadlineMetadata(schedule, phase, studentUuid),
                    "assignment-deadline-reminder:" + phase + ":" + schedule.getUuid() + ":" + studentUuid
            ));
        }
    }

    private String buildBody(String phase) {
        if ("one_day".equals(phase)) {
            return "Your assignment is due in one day.";
        }
        return "Your assignment is due now.";
    }

    private Map<String, Object> deadlineMetadata(ClassAssignmentSchedule schedule, String phase, UUID studentUuid) {
        return Map.of(
                "schedule_uuid", schedule.getUuid() == null ? "" : schedule.getUuid().toString(),
                "class_definition_uuid", schedule.getClassDefinitionUuid() == null ? "" : schedule.getClassDefinitionUuid().toString(),
                "assignment_uuid", schedule.getAssignmentUuid() == null ? "" : schedule.getAssignmentUuid().toString(),
                "student_uuid", studentUuid == null ? "" : studentUuid.toString(),
                "due_at", schedule.getDueAt() == null ? "" : schedule.getDueAt().toString(),
                "phase", phase
        );
    }
}
