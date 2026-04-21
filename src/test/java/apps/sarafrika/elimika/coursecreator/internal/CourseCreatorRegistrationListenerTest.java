package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
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
class CourseCreatorRegistrationListenerTest {

    @Mock
    private CourseCreatorRepository courseCreatorRepository;

    @InjectMocks
    private CourseCreatorRegistrationListener listener;

    @Test
    void shouldCreateCourseCreatorProfileWhenDomainAssigned() {
        UUID userUuid = UUID.randomUUID();
        when(courseCreatorRepository.existsByUserUuid(userUuid)).thenReturn(false);

        listener.onUserDomainAssigned(new UserDomainMappingEvent(userUuid, "course_creator"));

        verify(courseCreatorRepository).save(argThat(matchesCourseCreator(userUuid)));
    }

    @Test
    void shouldIgnoreNonCourseCreatorDomainAssignments() {
        listener.onUserDomainAssigned(new UserDomainMappingEvent(UUID.randomUUID(), "student"));

        verifyNoInteractions(courseCreatorRepository);
    }

    @Test
    void shouldDeleteCourseCreatorProfileWhenDomainRemoved() {
        UUID userUuid = UUID.randomUUID();
        CourseCreator courseCreator = new CourseCreator();
        courseCreator.setUserUuid(userUuid);
        when(courseCreatorRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(courseCreator));

        listener.onUserDomainRemoved(new UserDomainRemovedEvent(userUuid, "course_creator"));

        verify(courseCreatorRepository).delete(courseCreator);
    }

    private ArgumentMatcher<CourseCreator> matchesCourseCreator(UUID userUuid) {
        return courseCreator -> courseCreator != null
                && userUuid.equals(courseCreator.getUserUuid())
                && "Unknown".equals(courseCreator.getFullName())
                && Boolean.FALSE.equals(courseCreator.getAdminVerified());
    }
}
