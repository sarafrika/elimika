package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.dto.OrganisationUserCreateRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks the invite-only rule for students.
 * <p>
 * This path provisions a Keycloak account and an active affiliation in one shot. That is
 * acceptable for staff an organisation employs, but for a learner it would hand the
 * organisation access to their work without the learner ever agreeing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceStudentCutoverTest {

    private static final UUID ORGANISATION_UUID = UUID.randomUUID();

    @Mock private OrganisationRepository organisationRepository;
    @Mock private UserDomainRepository userDomainRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;

    @InjectMocks private AdminServiceImpl adminService;

    @Test
    void studentsCannotBeCreatedDirectlyAndMustBeInvited() {
        when(organisationRepository.findByUuid(ORGANISATION_UUID))
                .thenReturn(Optional.of(new Organisation()));

        assertThatThrownBy(() -> adminService.createOrganisationUser(ORGANISATION_UUID, request("student")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be created directly")
                .hasMessageContaining("invitation");

        // Nothing may be provisioned on the rejected path.
        verify(userRepository, never()).save(any());
        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    @Test
    void theRejectionIsCaseInsensitive() {
        when(organisationRepository.findByUuid(ORGANISATION_UUID))
                .thenReturn(Optional.of(new Organisation()));

        assertThatThrownBy(() -> adminService.createOrganisationUser(ORGANISATION_UUID, request("STUDENT")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adminService.createOrganisationUser(ORGANISATION_UUID, request(" Student ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void staffDomainsAreUnaffectedAndStillResolveTheirDomain() {
        when(organisationRepository.findByUuid(ORGANISATION_UUID))
                .thenReturn(Optional.of(new Organisation()));
        UserDomain instructorDomain = new UserDomain();
        instructorDomain.setUuid(UUID.randomUUID());
        instructorDomain.setDomainName("instructor");
        when(userDomainRepository.findByDomainNameAndOrgSupportedTrue("instructor"))
                .thenReturn(Optional.of(instructorDomain));

        // Proceeds past the student guard; it fails later for unrelated reasons in this
        // unit context, which is enough to prove staff creation is not blocked here.
        assertThatThrownBy(() -> adminService.createOrganisationUser(ORGANISATION_UUID, request("instructor")))
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    private OrganisationUserCreateRequestDTO request(String domainName) {
        return new OrganisationUserCreateRequestDTO(
                "Jane", null, "Doe", "jane.doe@example.com", null, domainName, null);
    }
}
