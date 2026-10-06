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
import apps.sarafrika.elimika.tenancy.repository.OrganisationDocumentRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
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

/** Organisation steps: an organisation the user administers, with its details and required documents. */
@Component
@RequiredArgsConstructor
public class OrganisationOnboardingSteps implements OnboardingStepProvider {

    static final String APPLIES_TO = "ORGANISATION";

    private final UserDomainRepository domainRepository;
    private final UserOrganisationDomainMappingRepository organisationMappingRepository;
    private final OrganisationRepository organisationRepository;
    private final OrganisationDocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;

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
            if (missingDetails.size() + missingDocuments.size() < detailsMissing.size() + documentsMissing.size()
                    || detailsMissing.contains("organisation")) {
                detailsMissing = missingDetails;
                documentsMissing = missingDocuments;
                uploaded = documents.size();
            }
        }
        return List.of(
                OnboardingStep.of(40, "organisation", "Organisation details", true, false, detailsMissing),
                OnboardingStep.of(50, "organisation_documents", "Organisation documents", true, false, documentsMissing)
                        .withCounts(Map.of("uploaded", uploaded, "required", (long) required.size())));
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
