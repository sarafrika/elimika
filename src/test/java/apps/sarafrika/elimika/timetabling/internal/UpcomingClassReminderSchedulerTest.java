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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcomingClassReminderSchedulerTest {

    @Mock
    private ScheduledInstanceRepository scheduledInstanceRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UpcomingClassReminderScheduler scheduler;

    @Test
    void remindersLinkEachRecipientToTheirOwnDashboardClassPage() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID studentUserUuid = UUID.randomUUID();

        ScheduledInstance instance = new ScheduledInstance();
        instance.setUuid(UUID.randomUUID());
        instance.setClassDefinitionUuid(classDefinitionUuid);
        instance.setInstructorUuid(instructorUuid);
        instance.setStartTime(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUuid(studentUuid);

        when(scheduledInstanceRepository.findScheduledInstancesStartingBetween(any(), any()))
                .thenReturn(List.of(instance));
        when(classDefinitionLookupService.findByUuid(classDefinitionUuid))
                .thenReturn(Optional.of(snapshot(classDefinitionUuid)));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructorUserUuid));
        when(enrollmentRepository.findByScheduledInstanceUuidAndStatus(instance.getUuid(), EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(studentLookupService.getStudentUserUuid(studentUuid)).thenReturn(Optional.of(studentUserUuid));

        scheduler.sendUpcomingClassReminders();

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        NotificationRequestedEvent instructorEvent = captor.getAllValues().get(0);
        NotificationRequestedEvent studentEvent = captor.getAllValues().get(1);

        assertThat(instructorEvent.recipientId()).isEqualTo(instructorUserUuid);
        assertThat(instructorEvent.actionUrl())
                .isEqualTo("/dashboard/instructor/classes/class-training/" + classDefinitionUuid);
        assertThat(instructorEvent.recipientDomain()).isEqualTo("instructor");

        assertThat(studentEvent.recipientId()).isEqualTo(studentUserUuid);
        assertThat(studentEvent.actionUrl()).isEqualTo("/dashboard/student/schedule/classes/" + classDefinitionUuid);
    }

    private ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot(UUID classDefinitionUuid) {
        return new ClassDefinitionLookupService.ClassDefinitionSnapshot(
                classDefinitionUuid, null, null, "Algebra", null, null, null, null,
                null, null, null, null, 30, null, null);
    }
}
