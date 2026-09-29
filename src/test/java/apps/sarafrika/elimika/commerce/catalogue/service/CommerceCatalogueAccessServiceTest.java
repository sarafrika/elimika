package apps.sarafrika.elimika.commerce.catalogue.service;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService.VisibilityContext;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommerceCatalogueAccessServiceTest {

    @Mock
    private DomainSecurityService domainSecurityService;

    @Test
    void signedInCallersSeeOnlyPublicActiveEntriesLikeAnonymousVisitors() {
        CommerceCatalogueAccessService service = new CommerceCatalogueAccessService(domainSecurityService);
        VisibilityContext signedIn = new VisibilityContext(true, false);

        assertThat(service.canView(item(true, true), signedIn)).isTrue();
        assertThat(service.canView(item(false, true), signedIn)).isFalse();
        assertThat(service.canView(item(true, false), signedIn)).isFalse();
        assertThat(service.canView(item(true, false), new VisibilityContext(false, false))).isFalse();
        assertThat(service.buildVisibilitySpecification(signedIn)).isNotNull();
    }

    @Test
    void platformAdminsSeeHiddenAndInactiveEntries() {
        CommerceCatalogueAccessService service = new CommerceCatalogueAccessService(domainSecurityService);
        VisibilityContext admin = new VisibilityContext(true, true);

        assertThat(service.canView(item(false, false), admin)).isTrue();
        assertThat(service.buildVisibilitySpecification(admin)).isNull();
    }

    private CommerceCatalogueItem item(boolean publiclyVisible, boolean active) {
        CommerceCatalogueItem item = new CommerceCatalogueItem();
        item.setPubliclyVisible(publiclyVisible);
        item.setActive(active);
        return item;
    }
}
