package apps.sarafrika.elimika.tenancy.internal.onboarding;

import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganisationOnboardingStepsTest {

    @Mock private UserDomainRepository domainRepository;
    @Mock private UserOrganisationDomainMappingRepository organisationMappingRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private OrganisationDocumentRepository documentRepository;
    @Mock private DocumentTypeRepository documentTypeRepository;

    private final UUID userUuid = UUID.randomUUID();
    private final UUID adminDomain = UUID.randomUUID();
    private final OnboardingSubject subject = new OnboardingSubject(userUuid, UserDomain.organisation_user, null);
    private final DocumentType registration = documentType("CERTIFICATE_OF_REGISTRATION", true);
    private OrganisationOnboardingSteps steps;

    @BeforeEach
    void setUp() {
        steps = new OrganisationOnboardingSteps(domainRepository, organisationMappingRepository, organisationRepository,
                documentRepository, documentTypeRepository);
        apps.sarafrika.elimika.tenancy.entity.UserDomain admin = new apps.sarafrika.elimika.tenancy.entity.UserDomain();
        admin.setUuid(adminDomain);
        admin.setDomainName("admin");
        when(domainRepository.findByDomainName("admin")).thenReturn(Optional.of(admin));
        when(documentTypeRepository.findByAppliesToIgnoreCase(eq("ORGANISATION"), any()))
                .thenReturn(List.of(registration, documentType("LICENCE_OR_ACCREDITATION", false)));
    }

    @Test
    void withoutAnOrganisationBothStepsAreMissing() {
        when(organisationMappingRepository.findByUserUuidAndDomainUuidAndActiveTrueAndDeletedFalse(userUuid, adminDomain))
                .thenReturn(List.of());

        List<OnboardingStep> result = steps.steps(subject);

        assertThat(result).extracting(OnboardingStep::key).containsExactly("organisation", "organisation_documents");
        assertThat(result.get(0).missing()).containsExactly("organisation");
        assertThat(result.get(1).missing()).containsExactly("CERTIFICATE_OF_REGISTRATION");
    }

    @Test
    void anAdministeredOrganisationWithDetailsAndRequiredDocumentsCompletesBothSteps() {
        Organisation organisation = administers("Acme Academy", "Kenya");
        OrganisationDocument certificate = new OrganisationDocument();
        certificate.setDocumentTypeUuid(registration.getUuid());
        certificate.setStatus(DocumentStatus.PENDING);
        when(documentRepository.findByOrganisationUuid(organisation.getUuid())).thenReturn(List.of(certificate));

        List<OnboardingStep> result = steps.steps(subject);

        assertThat(result).allMatch(OnboardingStep::complete);
        assertThat(result.get(1).counts()).containsEntry("uploaded", 1L).containsEntry("required", 1L);
    }

    @Test
    void aRejectedDocumentAndMissingCountryStayOutstanding() {
        Organisation organisation = administers("Acme Academy", null);
        OrganisationDocument rejected = new OrganisationDocument();
        rejected.setDocumentTypeUuid(registration.getUuid());
        rejected.setStatus(DocumentStatus.REJECTED);
        when(documentRepository.findByOrganisationUuid(organisation.getUuid())).thenReturn(List.of(rejected));

        List<OnboardingStep> result = steps.steps(subject);

        assertThat(result.get(0).missing()).containsExactly("country");
        assertThat(result.get(1).missing()).containsExactly("CERTIFICATE_OF_REGISTRATION");
    }

    @Test
    void submittingAsksForVerificationOfUnverifiedOrganisationsOnce() {
        Organisation organisation = administers("Acme Academy", "Kenya");

        steps.onSubmitted(subject);
        assertThat(organisation.getVerificationRequestedAt()).isNotNull();
        verify(organisationRepository).save(organisation);

        Organisation verified = administers("Verified Org", "Kenya");
        verified.setAdminVerified(true);
        steps.onSubmitted(subject);
        verify(organisationRepository, never()).save(verified);
    }

    private Organisation administers(String name, String country) {
        Organisation organisation = new Organisation();
        organisation.setUuid(UUID.randomUUID());
        organisation.setName(name);
        organisation.setCountry(country);
        UserOrganisationDomainMapping mapping = new UserOrganisationDomainMapping();
        mapping.setOrganisationUuid(organisation.getUuid());
        when(organisationMappingRepository.findByUserUuidAndDomainUuidAndActiveTrueAndDeletedFalse(userUuid, adminDomain))
                .thenReturn(List.of(mapping));
        when(organisationRepository.findByUuidIn(List.of(organisation.getUuid()))).thenReturn(List.of(organisation));
        return organisation;
    }

    private static DocumentType documentType(String name, boolean required) {
        DocumentType type = new DocumentType();
        type.setUuid(UUID.randomUUID());
        type.setName(name);
        type.setIsRequired(required);
        type.setAppliesTo("ORGANISATION");
        return type;
    }
}
