package apps.sarafrika.elimika.shared.spi.onboarding;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.util.List;

/** Implemented by each module that owns part of a domain's onboarding; tenancy merges the steps by position. */
public interface OnboardingStepProvider {

    boolean supports(UserDomain domain);

    List<OnboardingStep> steps(OnboardingSubject subject);

    /** Records module-owned submission state; runs in the submit transaction after the steps were validated. */
    default void onSubmitted(OnboardingSubject subject) {
    }
}
