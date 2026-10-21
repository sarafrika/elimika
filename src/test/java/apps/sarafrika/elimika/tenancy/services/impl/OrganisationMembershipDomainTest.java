package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Only a school admin operates the school dashboard (organisation_user); everyone else is community. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganisationMembershipDomainTest {

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
    @Mock private DomainApprovalService domainApprovalService;

    @InjectMocks private OrganisationServiceImpl service;

    private final UUID organisationUuid = UUID.randomUUID();
    private final User user = new User();

    @BeforeEach
    void schoolAndPerson() {
        user.setUuid(UUID.randomUUID());
        when(organisationRepository.findByUuidAndDeletedFalse(organisationUuid)).thenReturn(Optional.of(new Organisation()));
        when(userRepository.findByEmailIgnoreCase("person@school.test")).thenReturn(Optional.of(user));
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
        when(userOrganisationDomainMappingRepository.findActiveByUserAndOrganisation(user.getUuid(), organisationUuid))
                .thenReturn(Optional.empty());
        for (String name : new String[]{"student", "instructor", "parent", "admin", "organisation_user"}) {
            UserDomain domain = new UserDomain();
            domain.setUuid(UUID.randomUUID());
            domain.setDomainName(name);
            when(userDomainRepository.findByDomainNameAndOrgSupportedTrue(name)).thenReturn(Optional.of(domain));
            when(userDomainRepository.findByDomainName(name)).thenReturn(Optional.of(domain));
        }
    }

    @Test
    @DisplayName("adding a learner, trainer, parent or non-admin member grants no school dashboard domain")
    void nonStaffGetNoUmbrellaDomain() {
        for (String role : new String[]{"student", "instructor", "parent", "organisation_user"}) {
            service.inviteUserToOrganisation(organisationUuid, "person@school.test", role, null);
        }
        verify(domainApprovalService, never()).grant(any(), any());
    }

    @Test
    @DisplayName("adding or promoting a school admin grants the umbrella organisation_user domain")
    void staffGetTheUmbrellaDomain() {
        service.inviteUserToOrganisation(organisationUuid, "person@school.test", "admin", null);
        verify(domainApprovalService).grant(user.getUuid(), apps.sarafrika.elimika.shared.utils.enums.UserDomain.organisation_user);

        UserOrganisationDomainMapping mapping = new UserOrganisationDomainMapping();
        when(userOrganisationDomainMappingRepository.findActiveByUserAndOrganisation(user.getUuid(), organisationUuid))
                .thenReturn(Optional.of(mapping));
        service.updateUserRoleInOrganisation(organisationUuid, user.getUuid(), "student");
        verify(domainApprovalService, org.mockito.Mockito.times(1)).grant(any(), any());
    }
}
