package apps.sarafrika.elimika.shared.spi.onboarding;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.util.UUID;

/** The generic onboarding submit, implemented by tenancy, for legacy domain routes that delegate to it. */
public interface OnboardingSubmissionService {

    /** Validates the required steps and submits the domain; IllegalStateException when it cannot be submitted. */
    void submitOnboarding(UUID userUuid, UserDomain domain);
}
