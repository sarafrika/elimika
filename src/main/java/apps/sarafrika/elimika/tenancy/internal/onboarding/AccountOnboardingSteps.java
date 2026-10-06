package apps.sarafrika.elimika.tenancy.internal.onboarding;

import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** The shared account step: names, email and phone on the Keycloak mirror, done once for every domain. */
@Component
@RequiredArgsConstructor
public class AccountOnboardingSteps implements OnboardingStepProvider {

    public static final String KEY = "account";

    private final UserRepository userRepository;

    @Override
    public boolean supports(UserDomain domain) {
        return domain != UserDomain.admin;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        List<String> missing = new ArrayList<>();
        User user = userRepository.findByUuid(subject.userUuid()).orElse(null);
        if (user == null || isBlank(user.getFirstName())) {
            missing.add("first_name");
        }
        if (user == null || isBlank(user.getLastName())) {
            missing.add("last_name");
        }
        if (user == null || isBlank(user.getEmail())) {
            missing.add("email");
        }
        if (user == null || isBlank(user.getPhoneNumber())) {
            missing.add("phone_number");
        }
        return List.of(OnboardingStep.of(10, KEY, "Account details", true, true, missing));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
