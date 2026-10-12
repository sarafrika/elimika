package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassScheduleBatchServiceTest {

    @Mock
    private ClassDefinitionRepository classDefinitionRepository;
    @Mock
    private ObjectProvider<TimetableService> timetableServiceProvider;
    @Mock
    private TimetableService timetableService;

    private ClassScheduleBatchService service;

    @BeforeEach
    void setUp() {
        service = new ClassScheduleBatchService(classDefinitionRepository, timetableServiceProvider);
    }

    @Test
    void readsTheSchedulesOfExistingClassesInOneTimetableCall() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(classDefinitionRepository.findExistingUuids(new LinkedHashSet<>(List.of(known, unknown))))
                .thenReturn(List.of(known));
        when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        List<ScheduledInstanceDTO> sessions = List.of();
        when(timetableService.getScheduledInstancesForClassDefinitions(List.of(known))).thenReturn(sessions);

        assertThat(service.getSchedules(List.of(known, unknown, known))).isSameAs(sessions);
        verify(timetableService).getScheduledInstancesForClassDefinitions(List.of(known));
    }

    @Test
    void noExistingClassSkipsTheTimetable() {
        UUID unknown = UUID.randomUUID();
        when(classDefinitionRepository.findExistingUuids(any())).thenReturn(List.of());

        assertThat(service.getSchedules(List.of(unknown))).isEmpty();
        verify(timetableService, never()).getScheduledInstancesForClassDefinitions(any());
    }

    @Test
    void emptyAndOversizedBatchesAreRejected() {
        assertThatThrownBy(() -> service.getSchedules(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getSchedules(null)).isInstanceOf(IllegalArgumentException.class);
        List<UUID> tooMany = IntStream.rangeClosed(0, ClassScheduleBatchService.MAX_BATCH_SIZE)
                .mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> service.getSchedules(tooMany)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(classDefinitionRepository);
    }
}
