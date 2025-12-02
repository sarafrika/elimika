package apps.sarafrika.elimika.commerce.internal.service;

import java.util.UUID;

/**
 * Provisions internal commerce catalog entries when courses/classes are published.
 */
public interface CatalogueProvisioningService {

    /**
     * Ensures a class definition has a purchasable variant under its parent course product.
     *
     * @param classDefinitionUuid class definition identifier
     */
    void ensureClassIsPurchasable(UUID classDefinitionUuid);
}
