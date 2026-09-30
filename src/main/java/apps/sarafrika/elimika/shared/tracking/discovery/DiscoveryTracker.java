package apps.sarafrika.elimika.shared.tracking.discovery;

import java.util.List;
import java.util.UUID;

/**
 * Records what recommenders show, for evaluating recommendations afterwards. Other modules inject
 * this; the implementation lives in {@code shared.tracking.service}.
 * <p>
 * Tracking never fails the caller: a recording error is logged and swallowed, so a recommendation
 * response is never lost to its own telemetry. No query text is ever recorded.
 */
public interface DiscoveryTracker {

    /**
     * Records one impression per item for a recommendation response.
     *
     * @param userUuid         the user the items were shown to - the authenticated principal, never a parameter
     * @param surface          where they were shown, lowercase with underscores (e.g. {@code course_recommendations})
     * @param recommendationId the id returned with the response; clicks and dismissals quote it back
     * @param modelVersion     the scoring version that produced the ranking, or {@code null}
     * @param items            the items in the order shown
     */
    void recordImpressions(UUID userUuid, String surface, UUID recommendationId, String modelVersion,
                           List<DiscoveryImpression> items);
}
