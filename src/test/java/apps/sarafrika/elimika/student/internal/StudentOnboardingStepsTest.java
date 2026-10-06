package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianContact;
import apps.sarafrika.elimika.student.repository.StudentGuardianContactRepository;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentOnboardingStepsTest {

    @Mock private StudentRepository studentRepository;
    @Mock private StudentGuardianContactRepository contactRepository;
    @Mock private StudentGuardianLinkRepository linkRepository;
    @Mock private UserLookupService userLookupService;

    private final UUID userUuid = UUID.randomUUID();
    private final UUID studentUuid = UUID.randomUUID();
    private StudentOnboardingSteps steps;

    @BeforeEach
    void setUp() {
        steps = new StudentOnboardingSteps(studentRepository, contactRepository, linkRepository, userLookupService);
        Student student = new Student();
        student.setUuid(studentUuid);
        student.setUserUuid(userUuid);
        when(studentRepository.findByUuid(studentUuid)).thenReturn(Optional.of(student));
        when(contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(eq(studentUuid), any())).thenReturn(List.of());
        when(linkRepository.findByStudentUuidAndStatus(eq(studentUuid), any())).thenReturn(List.of());
    }

    @Test
    void aMinorMustNameAGuardian() {
        when(userLookupService.findUserAgeInYears(eq(userUuid), any())).thenReturn(OptionalInt.of(14));

        List<OnboardingStep> result = steps.steps(new OnboardingSubject(userUuid, UserDomain.student, studentUuid));

        assertThat(result).extracting(OnboardingStep::key).containsExactly("student_profile", "guardians");
        assertThat(result.get(0).complete()).isTrue();
        assertThat(result.get(1).required()).isTrue();
        assertThat(result.get(1).complete()).isFalse();

        StudentGuardianContact invited = new StudentGuardianContact();
        invited.setStatus(GuardianContactStatus.INVITED);
        when(contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(eq(studentUuid), any()))
                .thenReturn(List.of(invited));
        OnboardingStep guardians = steps.steps(new OnboardingSubject(userUuid, UserDomain.student, studentUuid)).get(1);
        assertThat(guardians.complete()).isTrue();
        assertThat(guardians.counts()).containsEntry("guardians", 1L);
    }

    @Test
    void anAdultNeedsNoGuardianAndAnUnknownAgeLeavesItOptional() {
        when(userLookupService.findUserAgeInYears(eq(userUuid), any())).thenReturn(OptionalInt.of(30));
        OnboardingStep adult = steps.steps(new OnboardingSubject(userUuid, UserDomain.student, studentUuid)).get(1);
        assertThat(adult.required()).isFalse();
        assertThat(adult.complete()).isTrue();

        when(userLookupService.findUserAgeInYears(eq(userUuid), any())).thenReturn(OptionalInt.empty());
        OnboardingStep unknown = steps.steps(new OnboardingSubject(userUuid, UserDomain.student, studentUuid)).get(1);
        assertThat(unknown.required()).isFalse();
        assertThat(unknown.complete()).isFalse();
    }

    @Test
    void withoutAStudentProfileTheProfileStepIsMissing() {
        when(userLookupService.findUserAgeInYears(eq(userUuid), any())).thenReturn(OptionalInt.empty());

        OnboardingStep profile = steps.steps(new OnboardingSubject(userUuid, UserDomain.student, null)).get(0);

        assertThat(profile.complete()).isFalse();
        assertThat(profile.missing()).containsExactly("student_profile");
    }
}
