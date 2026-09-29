package apps.sarafrika.elimika.tenancy.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("People in global search")
class PeopleGlobalSearchProviderTest {

    private final DomainSecurityService domainSecurityService = mock(DomainSecurityService.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final TenancyGlobalSearchProviders.People provider =
            new TenancyGlobalSearchProviders.People(domainSecurityService, userLookupService,
                    mock(apps.sarafrika.elimika.tenancy.repository.UserRepository.class));

    @Test
    @DisplayName("Anonymous callers and plain members never see people")
    void hiddenFromEveryoneElse() {
        assertThat(provider.scopeForCurrentCaller()).isEmpty();

        UUID member = UUID.randomUUID();
        UUID organisation = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(member);
        when(userLookupService.getActiveUserOrganizations(member)).thenReturn(List.of(organisation));
        assertThat(provider.scopeForCurrentCaller()).isEmpty();
    }

    @Test
    @DisplayName("A platform admin searches everyone on every attribute")
    void platformAdminUnrestricted() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(provider.scopeForCurrentCaller()).get().extracting(SearchScope::isUnrestricted).isEqualTo(true);
        assertThat(provider.searchOnForCurrentCaller()).isNull();
    }

    @Test
    @DisplayName("A manager searches active members of the organisations they manage, by name only")
    void managerScopedToManagedOrganisations() {
        UUID manager = UUID.randomUUID();
        UUID managed = UUID.randomUUID();
        UUID memberOnly = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(manager);
        when(userLookupService.getActiveUserOrganizations(manager)).thenReturn(List.of(managed, memberOnly));
        when(domainSecurityService.managesOrganisation(managed)).thenReturn(true);

        SearchScope scope = provider.scopeForCurrentCaller().orElseThrow();
        assertThat(scope.filter()).isEqualTo(SearchFilter.and(
                SearchFilter.in("organisation_uuids", List.of(managed)),
                SearchFilter.eq("active", true)));
        assertThat(provider.searchOnForCurrentCaller()).containsExactly("full_name", "first_name", "last_name");
    }
}
