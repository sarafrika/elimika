package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** The instructor's teaching location, kept on the shared profile basics so other domains see it too. */
@Component
@RequiredArgsConstructor
public class InstructorOnboardingSteps implements OnboardingStepProvider {

    private final ProfessionalProfileService profileService;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.instructor;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        ProfessionalProfileDTO basics = profileService.getBasics(subject.userUuid());
        boolean located = basics != null && basics.locationName() != null && !basics.locationName().isBlank();
        return List.of(OnboardingStep.of(40, "teaching_location", "Teaching location", true, true,
                located ? List.of() : List.of("location_name")));
    }
}
