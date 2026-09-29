package apps.sarafrika.elimika.commerce.catalogue.service;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Evaluates whether the current request context can view a catalogue item.
 * <p>
 * Catalogue entries are written only by platform admins, so they are the only callers who see
 * hidden or inactive entries. Every other caller, signed in or not, sees what the storefront
 * sells: entries that are both publicly visible and active.
 */
@Service
@RequiredArgsConstructor
public class CommerceCatalogueAccessService {

    private final DomainSecurityService domainSecurityService;

    public VisibilityContext buildContext() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        boolean authenticated = userUuid != null;
        boolean admin = authenticated && domainSecurityService.isPlatformAdmin();
        return new VisibilityContext(authenticated, admin);
    }

    /**
     * @return {@code null} when the caller may see every entry, otherwise the public-and-active filter
     */
    public Specification<CommerceCatalogueItem> buildVisibilitySpecification(VisibilityContext context) {
        if (context.admin()) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("publiclyVisible")),
                cb.isTrue(root.get("active")));
    }

    public boolean canView(CommerceCatalogueItem item, VisibilityContext context) {
        if (item == null) {
            return false;
        }
        if (context.admin()) {
            return true;
        }
        return item.isPubliclyVisible() && item.isActive();
    }

    public boolean canView(CommerceCatalogueItem item) {
        return canView(item, buildContext());
    }

    public record VisibilityContext(
            boolean authenticated,
            boolean admin) {
    }
}
