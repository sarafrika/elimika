package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceImplDomainTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDomainMappingRepository userDomainMappingRepository;
    @Mock
    private UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;

    @InjectMocks
    private UserLookupServiceImpl service;

    private final UUID userUuid = UUID.randomUUID();

    private static apps.sarafrika.elimika.tenancy.entity.UserDomain domainEntity(String name) {
        apps.sarafrika.elimika.tenancy.entity.UserDomain d = new apps.sarafrika.elimika.tenancy.entity.UserDomain();
        d.setDomainName(name);
        return d;
    }

    private static UserDomainMapping globalMapping(String name) {
        UserDomainMapping m = new UserDomainMapping();
        m.setUserDomain(domainEntity(name));
        return m;
    }

    private static UserOrganisationDomainMapping orgMapping(String name) {
        UserOrganisationDomainMapping m = new UserOrganisationDomainMapping();
        m.setDomain(domainEntity(name));
        return m;
    }

    @Test
    void matchesWhenUserHasGlobalDomain() {
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("instructor")));
        lenient().when(userOrganisationDomainMappingRepository.findByUserUuidAndActiveTrueAndDeletedFalse(userUuid))
                .thenReturn(List.of());

        assertThat(service.userHasDomain(userUuid, UserDomain.instructor)).isTrue();
    }

    @Test
    void matchesWhenUserHasOnlyOrgScopedRole() {
        // Org member: globally only the organisation_user umbrella, org-scoped admin.
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("organisation_user")));
        when(userOrganisationDomainMappingRepository.findByUserUuidAndActiveTrueAndDeletedFalse(userUuid))
                .thenReturn(List.of(orgMapping("admin")));

        assertThat(service.userHasDomain(userUuid, UserDomain.admin)).isTrue();
    }

    @Test
    void doesNotMatchInactiveOrDeletedOrgMapping() {
        // The active/deleted filtering is done by the repository query; an inactive
        // mapping is simply not returned, so it never counts toward the check.
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("organisation_user")));
        when(userOrganisationDomainMappingRepository.findByUserUuidAndActiveTrueAndDeletedFalse(userUuid))
                .thenReturn(List.of());

        assertThat(service.userHasDomain(userUuid, UserDomain.admin)).isFalse();
    }

    @Test
    void doesNotMatchUnrelatedDomain() {
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("organisation_user")));
        when(userOrganisationDomainMappingRepository.findByUserUuidAndActiveTrueAndDeletedFalse(userUuid))
                .thenReturn(List.of(orgMapping("student")));

        assertThat(service.userHasDomain(userUuid, UserDomain.instructor)).isFalse();
    }

    @Test
    void globalDomainCheckIgnoresOrgScopedRoles() {
        // Platform-admin rail: org-scoped admin must NOT satisfy the global check.
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("organisation_user")));

        assertThat(service.userHasGlobalDomain(userUuid, UserDomain.admin)).isFalse();
    }

    @Test
    void globalDomainCheckMatchesGlobalAdmin() {
        when(userDomainMappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(globalMapping("admin")));

        assertThat(service.userHasGlobalDomain(userUuid, UserDomain.admin)).isTrue();
    }
}
