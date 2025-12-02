package apps.sarafrika.elimika.commerce.internal.service;

/**
 * Resolves region codes for commerce operations without relying on client-supplied values.
 */
public interface RegionResolver {

    /**
     * Resolves a normalized region code to apply to commerce operations.
     *
     * @param requestedRegion optional region hint from upstream (ignored if a default is configured)
     * @return normalized region code (e.g., ISO country code)
     */
    String resolveRegionCode(String requestedRegion);
}
