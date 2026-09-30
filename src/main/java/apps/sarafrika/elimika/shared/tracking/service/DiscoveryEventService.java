package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryEventType;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.shared.tracking.entity.DiscoveryEvent;
import apps.sarafrika.elimika.shared.tracking.repository.DiscoveryEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes discovery events. Impressions come from recommenders through {@link DiscoveryTracker};
 * clicks and dismissals come from the client through {@link #recordClientEvent}, and are kept only
 * when they refer to an impression the same user was really shown, from which they inherit the
 * surface, reason codes and model version.
 * <p>
 * Every identifier stored here is checked against a narrow pattern, so nothing free-text - and in
 * particular no query text - can reach the table.
 */
@Slf4j
@Service
public class DiscoveryEventService implements DiscoveryTracker {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9_]{1,64}");
    private static final Pattern REASON_CODE = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private static final Pattern MODEL_VERSION = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final int MAX_IMPRESSIONS = 200;
    private static final int MAX_REASON_CODES = 16;

    private final DiscoveryEventRepository repository;
    private final TransactionTemplate ownTransaction;
    private final Clock clock;

    @Autowired
    public DiscoveryEventService(DiscoveryEventRepository repository, PlatformTransactionManager transactionManager) {
        this(repository, transactionManager, Clock.systemUTC());
    }

    DiscoveryEventService(DiscoveryEventRepository repository, PlatformTransactionManager transactionManager,
                          Clock clock) {
        this.repository = repository;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Override
    public void recordImpressions(UUID userUuid, String surface, UUID recommendationId, String modelVersion,
                                  List<DiscoveryImpression> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        try {
            List<DiscoveryEvent> events = impressions(userUuid, surface, recommendationId, modelVersion, items);
            ownTransaction.executeWithoutResult(status -> repository.saveAll(events));
        } catch (RuntimeException ex) {
            // Telemetry must never cost the caller its response.
            log.warn("Could not record {} discovery impression(s) for surface {}: {}",
                    items.size(), surface, ex.getMessage());
        }
    }

    /**
     * Records a click or dismissal reported by the client for {@code userUuid}, which must come from
     * the authenticated principal.
     *
     * @return {@code true} when recorded; {@code false} when no matching impression was shown to this user
     * @throws IllegalArgumentException for an impression reported by the client, or a malformed field
     */
    public boolean recordClientEvent(UUID userUuid, UUID recommendationId, UUID itemUuid, String itemType,
                                     DiscoveryEventType eventType, int position) {
        Objects.requireNonNull(userUuid, "userUuid");
        if (recommendationId == null || itemUuid == null) {
            throw new IllegalArgumentException("recommendation_id and item_uuid are required");
        }
        if (eventType == null || eventType == DiscoveryEventType.IMPRESSION) {
            throw new IllegalArgumentException("event_type must be CLICK or DISMISS; impressions are recorded by the server");
        }
        requireSlug(itemType, "item_type");
        requirePosition(position);

        Optional<DiscoveryEvent> impression = repository
                .findFirstByUserUuidAndRecommendationIdAndItemUuidAndEventTypeOrderByIdAsc(
                        userUuid, recommendationId, itemUuid, DiscoveryEventType.IMPRESSION);
        if (impression.isEmpty() || !impression.get().getItemType().equals(itemType)) {
            log.debug("Dropped a {} for recommendation {} with no matching impression", eventType, recommendationId);
            return false;
        }
        DiscoveryEvent shown = impression.get();
        repository.save(DiscoveryEvent.of(userUuid, shown.getSurface(), recommendationId, itemType, itemUuid,
                position, eventType, shown.getReasonCodes(), shown.getModelVersion(), now()));
        return true;
    }

    /** Deletes up to {@code limit} events created before {@code cutoff}, in a transaction of its own. */
    public int purgeBatch(LocalDateTime cutoff, int limit) {
        Integer deleted = ownTransaction.execute(status -> repository.deleteOlderThan(cutoff, limit));
        return deleted == null ? 0 : deleted;
    }

    LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private List<DiscoveryEvent> impressions(UUID userUuid, String surface, UUID recommendationId,
                                             String modelVersion, List<DiscoveryImpression> items) {
        Objects.requireNonNull(userUuid, "userUuid");
        Objects.requireNonNull(recommendationId, "recommendationId");
        requireSlug(surface, "surface");
        if (modelVersion != null && !MODEL_VERSION.matcher(modelVersion).matches()) {
            throw new IllegalArgumentException("model_version must match " + MODEL_VERSION.pattern());
        }
        if (items.size() > MAX_IMPRESSIONS) {
            throw new IllegalArgumentException("At most " + MAX_IMPRESSIONS + " impressions per response");
        }
        LocalDateTime now = now();
        return items.stream().map(item -> {
            requireSlug(item.itemType(), "item_type");
            requirePosition(item.position());
            if (item.reasonCodes().size() > MAX_REASON_CODES
                    || item.reasonCodes().stream().anyMatch(code -> code == null || !REASON_CODE.matcher(code).matches())) {
                throw new IllegalArgumentException("reason codes must be at most " + MAX_REASON_CODES
                        + " values matching " + REASON_CODE.pattern());
            }
            return DiscoveryEvent.of(userUuid, surface, recommendationId, item.itemType(), item.itemUuid(),
                    item.position(), DiscoveryEventType.IMPRESSION, item.reasonCodes().toArray(String[]::new),
                    modelVersion, now);
        }).toList();
    }

    private static void requireSlug(String value, String field) {
        if (value == null || !SLUG.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be 1-64 lowercase letters, digits or underscores");
        }
    }

    private static void requirePosition(int position) {
        if (position < 0 || position > 10_000) {
            throw new IllegalArgumentException("position must be between 0 and 10000");
        }
    }
}
