package apps.sarafrika.elimika.commerce.spi.paywall;

import java.util.UUID;

/**
 * Commerce Paywall Service Provider Interface
 * <p>
 * Provides access verification for commerce-gated resources like class enrollments.
 * This interface is exposed by the commerce module for other modules to verify
 * whether a student has purchased access to specific resources.
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