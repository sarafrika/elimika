package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the organisation verification lifecycle: an organisation submits itself
 * for review, an admin then verifies or unverifies it.
 */
@ExtendWith(MockitoExtension.class)
class OrganisationVerificationServiceTest {

    private static final UUID ORG_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserDomainRepository userDomainRepository;
    @Mock private UserDomainMappingRepository userDomainMappingRepository;
    @Mock private UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    @Mock private TrainingBranchRepository trainingBranchRepository;
    @Mock private GenericSpecificationBuilder<Organisation> specificationBuilder;
    @Mock private UserContextService userContextService;
    @Mock private DomainSecurityService domainSecurityService;

    @InjectMocks private OrganisationServiceImpl service;

    private Organisation organisation(boolean verified) {
        Organisation organisation = new Organisation();
        organisation.setUuid(ORG_UUID);
        organisation.setName("Sarafrika Training Centre");
        organisation.setAdminVerified(verified);
        return organisation;
    }

    @Test
    void requestVerificationRecordsTheSubmissionTimestamp() {
        Organisation organisation = organisation(false);
        when(organisationRepository.findByUuidAndDeletedFalse(ORG_UUID)).thenReturn(Optional.of(organisation));
        when(organisationRepository.save(any(Organisation.class))).thenAnswer(i -> i.getArgument(0));

        // The service stamps UTC, so the bound must be UTC too — a local-time bound
        // fails in any zone ahead of UTC (e.g. EAT, UTC+3).
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        OrganisationDTO result = service.requestOrganisationVerification(ORG_UUID);

        assertThat(organisation.getVerificationRequestedAt()).isNotNull().isAfter(before);
        assertThat(result.verificationRequestedAt()).isEqualTo(organisation.getVerificationRequestedAt());
        assertThat(result.adminVerified()).isFalse();
        verify(organisationRepository).save(organisation);
    }

    @Test
    void requestVerificationIsANoOpForAnAlreadyVerifiedOrganisation() {
        Organisation organisation = organisation(true);
        when(organisationRepository.findByUuidAndDeletedFalse(ORG_UUID)).thenReturn(Optional.of(organisation));

        OrganisationDTO result = service.requestOrganisationVerification(ORG_UUID);

        assertThat(organisation.getVerificationRequestedAt()).isNull();
        assertThat(result.adminVerified()).isTrue();
        verify(organisationRepository, never()).save(any(Organisation.class));
    }

    @Test
    void verifyOrganisationSetsTheAdminVerifiedFlag() {
        Organisation organisation = organisation(false);
        when(organisationRepository.findByUuidAndDeletedFalse(ORG_UUID)).thenReturn(Optional.of(organisation));
        when(organisationRepository.save(any(Organisation.class))).thenAnswer(i -> i.getArgument(0));

        OrganisationDTO result = service.verifyOrganisation(ORG_UUID, "Documents checked");

        assertThat(result.adminVerified()).isTrue();
        verify(domainSecurityService).enforceNotSelfApprovingOrganisation(ORG_UUID);
        verify(organisationRepository).save(organisation);
    }

    @Test
    void unverifyOrganisationClearsTheAdminVerifiedFlag() {
        Organisation organisation = organisation(true);
        when(organisationRepository.findByUuidAndDeletedFalse(ORG_UUID)).thenReturn(Optional.of(organisation));
        when(organisationRepository.save(any(Organisation.class))).thenAnswer(i -> i.getArgument(0));

        OrganisationDTO result = service.unverifyOrganisation(ORG_UUID, "Licence expired");

        assertThat(result.adminVerified()).isFalse();
        verify(organisationRepository).save(organisation);
    }
}
