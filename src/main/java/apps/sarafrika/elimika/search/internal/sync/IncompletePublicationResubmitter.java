package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.shared.search.SearchIndexRequested;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries indexing requests whose sync failed - typically because the engine was down.
 * <p>
 * Uses {@link IncompleteEventPublications#resubmitIncompletePublications} with a predicate narrowed to
 * {@link SearchIndexRequested} publications older than {@code search.resubmit-older-than}, so other
 * modules' incomplete publications are left to their own jobs and a sync still in flight is not
 * raced. Modulith also republishes everything outstanding on restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class IncompletePublicationResubmitter {

    private final IncompleteEventPublications incompleteEventPublications;
    private final SearchProperties properties;

    @Scheduled(
            initialDelayString = "${search.resubmit-initial-delay:PT2M}",
            fixedDelayString = "${search.resubmit-interval:PT5M}")
    public void resubmit() {
        try {
            incompleteEventPublications.resubmitIncompletePublications(this::isStuckIndexRequest);
        } catch (Exception ex) {
            log.error("Failed to resubmit incomplete search index publications: {}", ex.getMessage(), ex);
        }
    }

    boolean isStuckIndexRequest(EventPublication publication) {
        if (!(publication.getEvent() instanceof SearchIndexRequested request)) {
            return false;
        }
        boolean stuck = publication.getPublicationDate()
                .isBefore(Instant.now().minus(properties.getResubmitOlderThan()));
        if (stuck) {
            log.info("Resubmitting search index request {} for {} published at {}",
                    publication.getIdentifier(), request.index(), publication.getPublicationDate());
        }
        return stuck;
    }
}
