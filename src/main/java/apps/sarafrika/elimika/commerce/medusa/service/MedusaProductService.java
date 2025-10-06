package apps.sarafrika.elimika.commerce.medusa.service;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaDigitalProductResponse;

/**
 * Defines interactions with Medusa required for managing products.
 */
public interface MedusaProductService {

    /**
     * Creates a digital product with a single SKU on the Medusa admin API.
     *
     * @param request the product definition to create
     * @return the persisted Medusa product
     */
    MedusaDigitalProductResponse createDigitalProduct(MedusaDigitalProductRequest request);
}
