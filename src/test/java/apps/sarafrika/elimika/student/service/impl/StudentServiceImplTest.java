package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.exception.StudentAgeGateException;
import apps.sarafrika.elimika.student.factory.StudentFactory;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GenericSpecificationBuilder<Student> specificationBuilder;

    @Mock
    private RuleEvaluationService ruleEvaluationService;

    @Mock
    private UserLookupService userLookupService;

    @InjectMocks
    private StudentServiceImpl studentService;

    private StudentDTO request;

    @BeforeEach
    void setUp() {
        request = new StudentDTO(
                null,
                UUID.randomUUID(),
                "youth",
                "Guardian One",
                "+254700000000",
                "Guardian Two",
                "+254711111111",
                LocalDateTime.now(),
                "system",
                LocalDateTime.now(),
                "system"
        );
    }

    @Test
    void shouldThrowExceptionWhenAgeGateBlocks() {
        when(ruleEvaluationService.evaluateAgeGate(any(), any()))
                .thenReturn(AgeGateDecision.rejected("Too young"));
        when(userLookupService.getUserDateOfBirth(request.userUuid()))
                .thenReturn(Optional.of(LocalDate.of(2010, 5, 12)));

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(StudentAgeGateException.class)
                .hasMessageContaining("Too young");

        verifyNoInteractions(studentRepository);
    }

    @Test
    void shouldPersistStudentWhenAgeGateAllows() {
        when(ruleEvaluationService.evaluateAgeGate(any(), any()))
                .thenReturn(AgeGateDecision.allow());
        when(userLookupService.getUserDateOfBirth(request.userUuid()))
                .thenReturn(Optional.of(LocalDate.of(2010, 5, 12)));
        Student persisted = StudentFactory.toEntity(request);
        persisted.setUuid(UUID.randomUUID());
        when(studentRepository.save(any(Student.class))).thenReturn(persisted);

        StudentDTO response = studentService.createStudent(request);

        assertThat(response.demographicTag()).isEqualTo(request.demographicTag());
        verify(studentRepository).save(any(Student.class));
    }
}
