package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.event.user.DomainModeratedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstructorDomainModerationListenerTest {

    @Mock private InstructorRepository instructorRepository;

    private final UUID userUuid = UUID.randomUUID();
    private Instructor instructor;
    private InstructorDomainModerationListener listener;

    @BeforeEach
    void setUp() {
        listener = new InstructorDomainModerationListener(instructorRepository);
        instructor = new Instructor();
        instructor.setUuid(UUID.randomUUID());
        instructor.setUserUuid(userUuid);
        instructor.setAdminVerified(false);
        when(instructorRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(instructor));
    }

    @Test
    void approvingTheInstructorDomainVerifiesTheInstructor() {
        listener.onDomainModerated(new DomainModeratedEvent(userUuid, "instructor", DomainApprovalStatus.APPROVED, null,
                UUID.randomUUID()));

        assertThat(instructor.getAdminVerified()).isTrue();
        verify(instructorRepository).save(instructor);
    }

    @Test
    void revokingTheDomainRemovesVerification() {
        instructor.setAdminVerified(true);

        listener.onDomainModerated(new DomainModeratedEvent(userUuid, "instructor", DomainApprovalStatus.SUSPENDED,
                "Fake credentials", UUID.randomUUID()));

        assertThat(instructor.getAdminVerified()).isFalse();
    }

    @Test
    void otherDomainsAreIgnored() {
        listener.onDomainModerated(new DomainModeratedEvent(userUuid, "student", DomainApprovalStatus.APPROVED, null, null));

        verify(instructorRepository, never()).save(any());
    }
}
