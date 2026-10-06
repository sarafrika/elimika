package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSummaryDTO;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The shared professional profile and skills wallet steps; one copy per user serves every professional domain. */
@Component
@RequiredArgsConstructor
public class ProfileOnboardingSteps implements OnboardingStepProvider {

    private final ProfessionalProfileService profileService;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.instructor || domain == UserDomain.course_creator;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        ProfileSummaryDTO summary = profileService.getSummary(subject.userUuid());
        ProfessionalProfileDTO basics = summary.basics();
        List<String> basicsMissing = new ArrayList<>();
        if (basics == null || basics.professionalHeadline() == null || basics.professionalHeadline().isBlank()) {
            basicsMissing.add("professional_headline");
        }
        if (basics == null || basics.bio() == null || basics.bio().isBlank()) {
            basicsMissing.add("bio");
        }
        Map<String, Long> counts = summary.sectionCounts() == null ? Map.of() : summary.sectionCounts();
        long skills = counts.getOrDefault("skills", 0L);
        return List.of(
                OnboardingStep.of(20, "professional_profile", "Professional profile", true, true, basicsMissing),
                OnboardingStep.of(30, "skills_wallet", "Skills wallet", true, true,
                        skills > 0 ? List.of() : List.of("skills")).withCounts(counts));
    }
}
