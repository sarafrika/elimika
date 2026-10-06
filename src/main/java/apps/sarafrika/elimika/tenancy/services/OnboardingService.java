package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.OnboardingDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingSummaryDTO;

import java.util.List;
import java.util.UUID;

/** One onboarding flow for every domain, built from the steps each domain module contributes. */
public interface OnboardingService {

    List<OnboardingSummaryDTO> domainsOf(UUID userUuid);

    OnboardingDTO onboarding(UUID userUuid, UserDomain domain);

    OnboardingDTO submit(UUID userUuid, UserDomain domain);
}
