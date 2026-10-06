package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentGuardianContactRepository;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Student steps: the student profile, and guardians (required only for a learner under the age of majority). */
@Component
@RequiredArgsConstructor
public class StudentOnboardingSteps implements OnboardingStepProvider {

    private final StudentRepository studentRepository;
    private final StudentGuardianContactRepository contactRepository;
    private final StudentGuardianLinkRepository linkRepository;
    private final UserLookupService userLookupService;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.student;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        Student student = subject.domainProfileUuid() == null ? null
                : studentRepository.findByUuid(subject.domainProfileUuid()).orElse(null);
        OptionalInt age = userLookupService.findUserAgeInYears(subject.userUuid(), LocalDate.now(ZoneOffset.UTC));
        boolean minor = age.isPresent() && age.getAsInt() < MinorLearnerLookupService.AGE_OF_MAJORITY;
        boolean adult = age.isPresent() && !minor;
        long guardians = student == null ? 0 : guardianCount(student);
        boolean guardiansDone = guardians > 0 || adult;
        return List.of(
                OnboardingStep.of(40, "student_profile", "Student profile", true, false,
                        student == null ? List.of("student_profile") : List.of()),
                OnboardingStep.of(50, "guardians", "Guardians", minor, false,
                        guardiansDone ? List.of() : List.of("guardian")).withCounts(Map.of("guardians", guardians)));
    }

    private long guardianCount(Student student) {
        long contacts = contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(student.getUuid(),
                        GuardianContactStatus.REMOVED).stream()
                .filter(contact -> contact.getStatus() != GuardianContactStatus.DECLINED)
                .count();
        if (contacts > 0) {
            return contacts;
        }
        long links = linkRepository.findByStudentUuidAndStatus(student.getUuid(), GuardianLinkStatus.ACTIVE).size();
        if (links > 0) {
            return links;
        }
        return hasText(student.getFirstGuardianName()) || hasText(student.getSecondGuardianName()) ? 1 : 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
