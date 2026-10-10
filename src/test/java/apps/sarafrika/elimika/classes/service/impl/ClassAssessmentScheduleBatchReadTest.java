package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassAssessmentSchedulesDTO;
import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.classes.repository.ClassQuizScheduleRepository;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassAssessmentScheduleBatchReadTest {

    @Mock
    private ClassAssignmentScheduleRepository classAssignmentScheduleRepository;
    @Mock
    private ClassQuizScheduleRepository classQuizScheduleRepository;
    @Mock
    private DomainSecurityService domainSecurityService;

    @InjectMocks
    private ClassAssessmentScheduleServiceImpl service;

    @Test
    void returnsSchedulesOfViewableClassesOnlyInTwoQueries() {
        UUID mine = UUID.randomUUID();
        UUID notMine = UUID.randomUUID();
        when(domainSecurityService.canViewClassSchedule(mine)).thenReturn(true);
        when(domainSecurityService.canViewClassSchedule(notMine)).thenReturn(false);

        ClassAssignmentSchedule assignment = new ClassAssignmentSchedule();
        assignment.setUuid(UUID.randomUUID());
        assignment.setClassDefinitionUuid(mine);
        ClassQuizSchedule quiz = new ClassQuizSchedule();
        quiz.setUuid(UUID.randomUUID());
        quiz.setClassDefinitionUuid(mine);
        when(classAssignmentScheduleRepository.findByClassDefinitionUuidIn(List.of(mine))).thenReturn(List.of(assignment));
        when(classQuizScheduleRepository.findByClassDefinitionUuidIn(List.of(mine))).thenReturn(List.of(quiz));

        ClassAssessmentSchedulesDTO result = service.getAssessmentSchedules(List.of(mine, notMine, mine));

        assertThat(result.assignmentSchedules()).singleElement()
                .satisfies(dto -> assertThat(dto.classDefinitionUuid()).isEqualTo(mine));
        assertThat(result.quizSchedules()).singleElement()
                .satisfies(dto -> assertThat(dto.classDefinitionUuid()).isEqualTo(mine));
    }

    @Test
    void nothingViewableSkipsTheQueriesAndReturnsEmptyLists() {
        UUID other = UUID.randomUUID();
        when(domainSecurityService.canViewClassSchedule(other)).thenReturn(false);

        ClassAssessmentSchedulesDTO result = service.getAssessmentSchedules(List.of(other));

        assertThat(result.assignmentSchedules()).isEmpty();
        assertThat(result.quizSchedules()).isEmpty();
        verify(classAssignmentScheduleRepository, never()).findByClassDefinitionUuidIn(any());
        verify(classQuizScheduleRepository, never()).findByClassDefinitionUuidIn(any());
    }

    @Test
    void rejectsAnEmptyBatch() {
        assertThatThrownBy(() -> service.getAssessmentSchedules(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMoreThanOneHundredClasses() {
        List<UUID> tooMany = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> service.getAssessmentSchedules(tooMany))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
        verify(domainSecurityService, never()).canViewClassSchedule(any());
    }
}
