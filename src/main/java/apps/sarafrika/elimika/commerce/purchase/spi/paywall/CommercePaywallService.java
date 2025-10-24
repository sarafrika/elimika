package apps.sarafrika.elimika.commerce.purchase.spi.paywall;

import java.util.UUID;

/**
 * Commerce Purchase Paywall Service
 * <p>
 * Provides access verification for commerce-gated resources.
 * Implementation is provided by commerce.purchase module.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-24
 */
public interface CommercePaywallService {

    /**
     * Verifies that a student has purchased access to enroll in a class.
     * Throws an exception if access is denied.
     *
     * @param studentUuid The UUID of the student attempting to enroll
     * @param classDefinitionUuid The UUID of the class definition they want to enroll in
     * @throws IllegalAccessException if the student hasn't purchased access
     */
    void verifyClassEnrollmentAccess(UUID studentUuid, UUID classDefinitionUuid);
}
