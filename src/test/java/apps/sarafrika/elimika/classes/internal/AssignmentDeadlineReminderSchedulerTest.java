package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentDeadlineReminderSchedulerTest {

    @Mock
    private ClassAssignmentScheduleRepository assignmentScheduleRepository;
    @Mock
    private TimetableService timetableService;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AssignmentDeadlineReminderScheduler scheduler;

    @Test
    void deadlineRemindersLinkToTheStudentAssignmentPage() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID assignmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        ClassAssignmentSchedule schedule = new ClassAssignmentSchedule();
        schedule.setUuid(UUID.randomUUID());
        schedule.setClassDefinitionUuid(classDefinitionUuid);
        schedule.setAssignmentUuid(assignmentUuid);
        schedule.setDueAt(LocalDateTime.now(ZoneOffset.UTC));

        when(assignmentScheduleRepository.findByDueAtBetween(any(), any())).thenReturn(List.of(schedule));
        when(timetableService.getActiveStudentUuidsForClass(classDefinitionUuid)).thenReturn(List.of(studentUuid));
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(UUID.randomUUID()));

        scheduler.sendAssignmentDeadlineReminders();

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationRequestedEvent::actionUrl)
                .containsOnly("/dashboard/student/assignment/" + assignmentUuid);
    }
}
