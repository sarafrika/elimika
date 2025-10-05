package apps.sarafrika.elimika.commerce.medusa.service;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerRequest;
import apps.sarafrika.elimika.commerce.medusa.dto.MedusaCustomerResponse;

/**
 * Provides access to customer-related Medusa operations.
 */
public interface MedusaCustomerService {

    /**
     * Ensures a Medusa customer exists for the supplied request, creating the customer if missing.
     *
     * @param request description of the customer to look up or create
     * @return the existing or newly created customer record
     */
    MedusaCustomerResponse ensureCustomer(MedusaCustomerRequest request);
}
