package apps.sarafrika.elimika.shared.spi.onboarding;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.util.UUID;

/** Whose onboarding is evaluated, for which domain, and that domain's profile row (null when none exists yet). */
public record OnboardingSubject(UUID userUuid, UserDomain domain, UUID domainProfileUuid) {
}
