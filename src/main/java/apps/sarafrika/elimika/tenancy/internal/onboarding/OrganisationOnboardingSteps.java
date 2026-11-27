package apps.sarafrika.elimika.tenancy.internal.onboarding;

import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubject;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationDocument;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.factory.OrganisationFactory;
import apps.sarafrika.elimika.tenancy.repository.OrganisationDocumentRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationPayoutDetailsRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.util.enums.OrganisationCapability;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Organisation steps: an organisation the user administers, with its details and required documents, and a
 * M-Pesa number to be paid on when it trains or funds.
 */
@Component
@RequiredArgsConstructor
public class OrganisationOnboardingSteps implements OnboardingStepProvider {

    static final String APPLIES_TO = "ORGANISATION";
    static final String PAYOUT_DETAILS = "payout_details";

    private final UserDomainRepository domainRepository;
    private final UserOrganisationDomainMappingRepository organisationMappingRepository;
    private final OrganisationRepository organisationRepository;
    private final OrganisationDocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final OrganisationPayoutDetailsRepository payoutDetailsRepository;

    @Override
    public boolean supports(UserDomain domain) {
        return domain == UserDomain.organisation_user;
    }

    @Override
    public List<OnboardingStep> steps(OnboardingSubject subject) {
        List<Organisation> organisations = administered(subject.userUuid());
        List<DocumentType> required = documentTypeRepository.findByAppliesToIgnoreCase(APPLIES_TO, Sort.by("name")).stream()
                .filter(type -> Boolean.TRUE.equals(type.getIsRequired()))
                .toList();

        List<String> detailsMissing = List.of("organisation");
        List<String> documentsMissing = required.stream().map(DocumentType::getName).toList();
        List<String> payoutMissing = List.of();
        boolean needsPayout = false;
        long uploaded = 0;
        // The best-prepared organisation counts: one the user administers is enough.
        for (Organisation organisation : organisations) {
            List<String> missingDetails = missingDetails(organisation);
            List<OrganisationDocument> documents = documentRepository.findByOrganisationUuid(organisation.getUuid()).stream()
                    .filter(document -> document.getStatus() != DocumentStatus.REJECTED)
                    .toList();
            Set<UUID> typesHeld = documents.stream().map(OrganisationDocument::getDocumentTypeUuid).collect(Collectors.toSet());
            List<String> missingDocuments = required.stream()
                    .filter(type -> !typesHeld.contains(type.getUuid()))
                    .map(DocumentType::getName)
                    .toList();
            boolean needsPayoutHere = needsPayout(organisation);
            List<String> missingPayout = needsPayoutHere
                    && !payoutDetailsRepository.existsByOrganisationUuid(organisation.getUuid())
                    ? List.of(PAYOUT_DETAILS) : List.of();
            if (missingDetails.size() + missingDocuments.size() + missingPayout.size()
                    < detailsMissing.size() + documentsMissing.size() + payoutMissing.size()
                    || detailsMissing.contains("organisation")) {
                detailsMissing = missingDetails;
                documentsMissing = missingDocuments;
                payoutMissing = missingPayout;
                needsPayout = needsPayoutHere;
                uploaded = documents.size();
            }
        }
        List<OnboardingStep> steps = new ArrayList<>(List.of(
                OnboardingStep.of(40, "organisation", "Organisation details", true, false, detailsMissing),
                OnboardingStep.of(50, "organisation_documents", "Organisation documents", true, false, documentsMissing)
                        .withCounts(Map.of("uploaded", uploaded, "required", (long) required.size()))));
        if (needsPayout) {
            steps.add(OnboardingStep.of(60, "organisation_payout", "Payout details", true, false, payoutMissing));
        }
        return List.copyOf(steps);
    }

    @Override
    public void onSubmitted(OnboardingSubject subject) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Organisation organisation : administered(subject.userUuid())) {
            if (!Boolean.TRUE.equals(organisation.getAdminVerified()) && organisation.getVerificationRequestedAt() == null) {
                organisation.setVerificationRequestedAt(now);
                organisationRepository.save(organisation);
            }
        }
    }

    private List<Organisation> administered(UUID userUuid) {
        UUID adminDomain = domainRepository.findByDomainName(UserDomain.admin.name())
                .map(apps.sarafrika.elimika.tenancy.entity.UserDomain::getUuid)
                .orElse(null);
        if (adminDomain == null) {
            return List.of();
        }
        List<UUID> organisationUuids = organisationMappingRepository
                .findByUserUuidAndDomainUuidAndActiveTrueAndDeletedFalse(userUuid, adminDomain).stream()
                .map(UserOrganisationDomainMapping::getOrganisationUuid)
                .distinct()
                .toList();
        if (organisationUuids.isEmpty()) {
            return List.of();
        }
        return organisationRepository.findByUuidIn(organisationUuids).stream()
                .filter(organisation -> !organisation.isDeleted())
                .sorted(Comparator.comparing(Organisation::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Only an organisation that will receive or pay out money needs somewhere to send it. */
    private static boolean needsPayout(Organisation organisation) {
        return OrganisationFactory.capabilities(organisation.getCapabilities()).stream()
                .anyMatch(OrganisationCapability.NEEDS_PAYOUT_DETAILS::contains);
    }

    private static List<String> missingDetails(Organisation organisation) {
        List<String> missing = new ArrayList<>();
        if (organisation.getName() == null || organisation.getName().isBlank()) {
            missing.add("name");
        }
        boolean hasCountry = organisation.getCountry() != null && !organisation.getCountry().isBlank();
        boolean hasLocation = organisation.getLocation() != null && !organisation.getLocation().isBlank();
        if (!hasCountry && !hasLocation) {
            missing.add("country");
        }
        return missing;
    }
}
