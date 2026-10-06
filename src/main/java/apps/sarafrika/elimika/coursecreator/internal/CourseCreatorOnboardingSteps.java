package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCategoryPreferenceRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Course creator steps: preferred course categories; on submit the creator's review moves to SUBMITTED. */
@Component
@RequiredArgsConstructor
public class CourseCreatorOnboardingSteps implements OnboardingStepProvider {

    private final CourseCreatorRepository courseCreatorRepository;
    private final CourseCreatorCategoryPreferenceRepository categoryPreferenceRepository;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.course_creator;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        long categories = subject.domainProfileUuid() == null ? 0
                : categoryPreferenceRepository.findByCourseCreatorUuid(subject.domainProfileUuid()).size();
        return List.of(OnboardingStep.of(40, "categories", "Course categories", true, false,
                categories > 0 ? List.of() : List.of("categories")).withCounts(Map.of("categories", categories)));
    }

    @Override
    public void onSubmitted(OnboardingSubject subject) {
        courseCreatorRepository.findByUserUuid(subject.userUuid()).ifPresent(this::markSubmitted);
    }

    private void markSubmitted(CourseCreator creator) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        creator.setVerificationStatus(CourseCreatorVerificationStatus.SUBMITTED);
        creator.setVerificationRequestedAt(now);
        creator.setSubmittedAt(now);
        creator.setReviewedAt(null);
        creator.setReviewReason(null);
        creator.setAdminVerified(false);
        courseCreatorRepository.save(creator);
    }
}
