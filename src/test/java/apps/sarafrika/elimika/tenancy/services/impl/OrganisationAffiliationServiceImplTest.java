package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Affiliating an instructor with the organisation that hired them.
 * <p>
 * The interesting behaviour is what happens when they are already a member. Assigning a domain
 * overwrites the one they hold, so a course creator who also picks up a class job must not be
 * silently demoted to instructor by the hire — the existing affiliation wins, and the hire simply
 * reports that it created nothing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Affiliating a hired instructor with the hiring organisation")
class OrganisationAffiliationServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private UserOrganisationDomainMappingRepository mappingRepository;

    private OrganisationAffiliationServiceImpl service;

    private UUID userUuid;
    private UUID organisationUuid;
    private UUID branchUuid;

    @BeforeEach
    void setUp() {
        service = new OrganisationAffiliationServiceImpl(userService, mappingRepository);
        userUuid = UUID.randomUUID();
        organisationUuid = UUID.randomUUID();
        branchUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("a new member is assigned the instructor domain and their consent is stamped")
    void affiliatesAndStampsConsent() {
        UserOrganisationDomainMapping created = new UserOrganisationDomainMapping();
        when(mappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));

        boolean affiliated = service.affiliateHiredInstructor(userUuid, organisationUuid, branchUuid);

        assertThat(affiliated).isTrue();
        verify(userService).assignUserToOrganisation(userUuid, organisationUuid, "instructor", branchUuid);

        ArgumentCaptor<UserOrganisationDomainMapping> saved =
                ArgumentCaptor.forClass(UserOrganisationDomainMapping.class);
        verify(mappingRepository).save(saved.capture());
        // Applying for the job is the consent — record it as such rather than as an admin invite.
        assertThat(saved.getValue().getConsentSource()).isEqualTo(ConsentSource.SELF_JOIN);
        assertThat(saved.getValue().getConsentGrantedByUserUuid()).isEqualTo(userUuid);
        assertThat(saved.getValue().getConsentGrantedAt()).isNotNull();
    }

    @Test
    @DisplayName("an existing member keeps the role they already hold")
    void doesNotOverwriteAnExistingAffiliation() {
        when(mappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid))
                .thenReturn(Optional.of(new UserOrganisationDomainMapping()));

        boolean affiliated = service.affiliateHiredInstructor(userUuid, organisationUuid, branchUuid);

        assertThat(affiliated).isFalse();
        verifyNoInteractions(userService);
        verify(mappingRepository, never()).save(any());
    }

    @Test
    @DisplayName("a hire with no user or no organisation is rejected rather than half-applied")
    void rejectsMissingIdentifiers() {
        assertThatThrownBy(() -> service.affiliateHiredInstructor(null, organisationUuid, branchUuid))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.affiliateHiredInstructor(userUuid, null, branchUuid))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userService);
        verifyNoInteractions(mappingRepository);
    }
}
