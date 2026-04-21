package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
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
class InstructorRegistrationListenerTest {

    @Mock
    private InstructorRepository instructorRepository;

    @InjectMocks
    private InstructorRegistrationListener listener;

    @Test
    void shouldCreateInstructorProfileWhenInstructorDomainAssigned() {
        UUID userUuid = UUID.randomUUID();
        when(instructorRepository.existsByUserUuid(userUuid)).thenReturn(false);

        listener.onUserDomainAssigned(new UserDomainMappingEvent(userUuid, "instructor"));

        verify(instructorRepository).save(argThat(matchesInstructor(userUuid)));
    }

    @Test
    void shouldIgnoreNonInstructorDomainAssignments() {
        listener.onUserDomainAssigned(new UserDomainMappingEvent(UUID.randomUUID(), "student"));

        verifyNoInteractions(instructorRepository);
    }

    @Test
    void shouldDeleteInstructorProfileWhenInstructorDomainRemoved() {
        UUID userUuid = UUID.randomUUID();
        Instructor instructor = new Instructor();
        instructor.setUserUuid(userUuid);
        when(instructorRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(instructor));

        listener.onUserDomainRemoved(new UserDomainRemovedEvent(userUuid, "instructor"));

        verify(instructorRepository).delete(instructor);
    }

    private ArgumentMatcher<Instructor> matchesInstructor(UUID userUuid) {
        return instructor -> instructor != null
                && userUuid.equals(instructor.getUserUuid())
                && Boolean.FALSE.equals(instructor.getAdminVerified());
    }
}
