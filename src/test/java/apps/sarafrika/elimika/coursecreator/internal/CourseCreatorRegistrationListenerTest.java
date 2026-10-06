package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import java.util.Optional;
import java.util.UUID;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
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

    @Mock
    private ProfessionalProfileService professionalProfileService;

    @InjectMocks
    private CourseCreatorRegistrationListener listener;

    @Test
    void shouldCreateCourseCreatorProfileWhenDomainAssigned() {
        UUID userUuid = UUID.randomUUID();
        when(courseCreatorRepository.existsByUserUuid(userUuid)).thenReturn(false);
        when(professionalProfileService.getBasics(userUuid)).thenReturn(new ProfessionalProfileDTO(userUuid,
                "Shared bio", "Shared headline", null, "Kisumu", null, null, null));

        listener.onUserDomainAssigned(new UserDomainMappingEvent(userUuid, "course_creator"));

        verify(courseCreatorRepository).save(argThat(matchesCourseCreator(userUuid)));
        verify(courseCreatorRepository).save(argThat(creator -> "Shared headline".equals(creator.getProfessionalHeadline())
                && "Kisumu".equals(creator.getLocationName())));
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
                && Boolean.FALSE.equals(courseCreator.getAdminVerified())
                && CourseCreatorVerificationStatus.DRAFT.equals(courseCreator.getVerificationStatus());
    }
}
