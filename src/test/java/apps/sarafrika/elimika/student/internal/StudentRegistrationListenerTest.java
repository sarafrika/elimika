package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.event.student.RegisterStudent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentRegistrationListenerTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserLookupService userLookupService;

    @InjectMocks
    private StudentRegistrationListener listener;

    @Test
    void shouldCreateStudentProfileWhenStudentDomainAssigned() {
        UUID userUuid = UUID.randomUUID();
        when(studentRepository.existsByUserUuid(userUuid)).thenReturn(false);
        when(userLookupService.getUserFullName(userUuid)).thenReturn(Optional.of("Wilfred Njuguna"));

        listener.onUserDomainAssigned(new UserDomainMappingEvent(userUuid, "student"));

        verify(studentRepository).save(argThat(matchesStudent(userUuid, "Wilfred Njuguna")));
    }

    @Test
    void shouldIgnoreNonStudentDomainAssignments() {
        listener.onUserDomainAssigned(new UserDomainMappingEvent(UUID.randomUUID(), "instructor"));

        verifyNoInteractions(studentRepository, userLookupService);
    }

    @Test
    void shouldCreateStudentProfileForLegacyRegisterStudentEvent() {
        UUID userUuid = UUID.randomUUID();
        when(studentRepository.existsByUserUuid(userUuid)).thenReturn(false);

        listener.onStudentRegistration(new RegisterStudent("Legacy Student", userUuid));

        verify(studentRepository).save(argThat(matchesStudent(userUuid, "Legacy Student")));
    }

    @Test
    void shouldDeleteStudentProfileWhenStudentDomainRemoved() {
        UUID userUuid = UUID.randomUUID();
        Student student = new Student();
        student.setUserUuid(userUuid);
        when(studentRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(student));

        listener.onUserDomainRemoved(new UserDomainRemovedEvent(userUuid, "student"));

        verify(studentRepository).delete(student);
    }

    private ArgumentMatcher<Student> matchesStudent(UUID userUuid, String fullName) {
        return student -> student != null
                && userUuid.equals(student.getUserUuid())
                && fullName.equals(student.getFullName());
    }
}
