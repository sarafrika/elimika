package apps.sarafrika.elimika.commerce.catalogue.service;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Evaluates whether the current request context can view a catalogue item.
 */
@Service
@RequiredArgsConstructor
public class CommerceCatalogueAccessService {

    private final DomainSecurityService domainSecurityService;

    public VisibilityContext buildContext() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        boolean authenticated = userUuid != null;
        boolean admin = domainSecurityService.isOrganizationAdmin();
        return new VisibilityContext(authenticated, admin);
    }

    public Specification<CommerceCatalogueItem> buildVisibilitySpecification(VisibilityContext context) {
        if (context.admin()) {
            return null;
        }
        if (!context.authenticated()) {
            return (root, query, cb) -> cb.isTrue(root.get("publiclyVisible"));
        }
        return null;
    }

    public boolean canView(CommerceCatalogueItem item, VisibilityContext context) {
        if (item == null) {
            return false;
        }
        if (item.isPubliclyVisible()) {
            return true;
        }
        if (!context.authenticated()) {
            return false;
        }
        if (context.admin()) {
            return true;
        }
        return true;
    }

    public boolean canView(CommerceCatalogueItem item) {
        return canView(item, buildContext());
    }

    public record VisibilityContext(
            boolean authenticated,
            boolean admin) {
    }
}
