package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassScheduleServiceImplTest {

    @Mock
    private ScheduledInstanceRepository scheduledInstanceRepository;

    @Test
    void getScheduleSummaryCalculatesProgressFromNonCancelledSessions() {
        UUID classDefinitionUuid = UUID.randomUUID();
        ClassScheduleServiceImpl service = new ClassScheduleServiceImpl(scheduledInstanceRepository);

        when(scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid))
                .thenReturn(List.of(
                        scheduledInstance(SchedulingStatus.COMPLETED, 60),
                        scheduledInstance(SchedulingStatus.ONGOING, 90),
                        scheduledInstance(SchedulingStatus.SCHEDULED, 120),
                        scheduledInstance(SchedulingStatus.CANCELLED, 60),
                        scheduledInstance(SchedulingStatus.BLOCKED, 60)
                ));

        var summary = service.getScheduleSummary(classDefinitionUuid);

        assertThat(summary.scheduledMinutes()).isEqualTo(270);
        assertThat(summary.scheduledInstances()).isEqualTo(3);
        assertThat(summary.completedSessions()).isEqualTo(1);
        assertThat(summary.classProgressPercentage()).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void getScheduleSummaryReturnsZeroProgressWhenNoSessionsExist() {
        UUID classDefinitionUuid = UUID.randomUUID();
        ClassScheduleServiceImpl service = new ClassScheduleServiceImpl(scheduledInstanceRepository);

        when(scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid)).thenReturn(List.of());

        var summary = service.getScheduleSummary(classDefinitionUuid);

        assertThat(summary.scheduledMinutes()).isZero();
        assertThat(summary.scheduledInstances()).isZero();
        assertThat(summary.completedSessions()).isZero();
        assertThat(summary.classProgressPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getScheduleSummariesGroupsOneQueryByClass() {
        UUID busyClass = UUID.randomUUID();
        UUID emptyClass = UUID.randomUUID();
        ClassScheduleServiceImpl service = new ClassScheduleServiceImpl(scheduledInstanceRepository);
        ScheduledInstance first = scheduledInstance(SchedulingStatus.COMPLETED, 60);
        ScheduledInstance second = scheduledInstance(SchedulingStatus.SCHEDULED, 90);
        first.setClassDefinitionUuid(busyClass);
        second.setClassDefinitionUuid(busyClass);

        when(scheduledInstanceRepository.findByClassDefinitionUuidIn(List.of(busyClass, emptyClass)))
                .thenReturn(List.of(first, second));

        var summaries = service.getScheduleSummaries(List.of(busyClass, emptyClass));

        assertThat(summaries.get(busyClass).scheduledMinutes()).isEqualTo(150);
        assertThat(summaries.get(busyClass).scheduledInstances()).isEqualTo(2);
        assertThat(summaries.get(busyClass).scheduledDays()).isEqualTo(1);
        assertThat(summaries.get(emptyClass).scheduledInstances()).isZero();
        verify(scheduledInstanceRepository, times(1)).findByClassDefinitionUuidIn(anyCollection());
    }

    private ScheduledInstance scheduledInstance(SchedulingStatus status, long minutes) {
        ScheduledInstance instance = new ScheduledInstance();
        instance.setUuid(UUID.randomUUID());
        instance.setStatus(status);
        instance.setStartTime(LocalDateTime.of(2026, 4, 28, 8, 0));
        instance.setEndTime(instance.getStartTime().plusMinutes(minutes));
        return instance;
    }
}
