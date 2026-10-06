package apps.sarafrika.elimika.availability.internal;

import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Optional instructor step: at least one open availability slot on the instructor's calendar. */
@Component
@RequiredArgsConstructor
public class AvailabilityOnboardingSteps implements OnboardingStepProvider {

    private final AvailabilityRepository availabilityRepository;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.instructor;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        long slots = subject.domainProfileUuid() == null ? 0
                : availabilityRepository.countByInstructorUuidAndIsAvailable(subject.domainProfileUuid(), true);
        return List.of(OnboardingStep.of(50, "availability", "Availability", false, false,
                slots > 0 ? List.of() : List.of("availability_slot")).withCounts(Map.of("slots", slots)));
    }
}
