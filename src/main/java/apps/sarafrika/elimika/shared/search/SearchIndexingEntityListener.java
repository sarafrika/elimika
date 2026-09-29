package apps.sarafrika.elimika.shared.search;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Turns entity writes into search indexing requests, so owning modules never have to remember to.
 * <p>
 * Registered on {@code BaseEntity} next to the audit listener. After every insert, update and delete
 * it looks up the triggers that the {@link SearchDocumentSource} beans declared for the entity's
 * class and enqueues the affected documents on {@link SearchIndexRequests}, which publishes them just
 * before the transaction commits.
 * <p>
 * Two hard rules, because this runs inside Hibernate's flush of a business transaction:
 * <ul>
 *     <li>It never throws. A broken trigger costs a stale search document (which reconciliation and
 *     rebuilds repair), never a failed business write.</li>
 *     <li>It never queries. Triggers are resolved once per entity class and cached; trigger functions
 *     only read state already on the entity.</li>
 * </ul>
 * Sources are resolved lazily through an {@link ObjectProvider}: this listener is created while the
 * entity manager factory is being built, and the sources depend on repositories that need it.
 */
@Slf4j
@Component
public class SearchIndexingEntityListener {

    private final ObjectProvider<SearchDocumentSource<?>> sources;
    private final ObjectProvider<SearchIndexRequests> requests;
    private final Map<Class<?>, List<IndexTrigger>> triggersByEntityClass = new ConcurrentHashMap<>();

    public SearchIndexingEntityListener(
            ObjectProvider<SearchDocumentSource<?>> sources,
            ObjectProvider<SearchIndexRequests> requests
    ) {
        this.sources = sources;
        this.requests = requests;
    }

    @PostPersist
    public void postPersist(Object entity) {
        onChange(entity);
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        onChange(entity);
    }

    @PostRemove
    public void postRemove(Object entity) {
        onChange(entity);
    }

    private void onChange(Object entity) {
        if (entity == null) {
            return;
        }
        try {
            SearchIndexRequests indexRequests = requests.getIfAvailable();
            if (indexRequests == null || !indexRequests.isEnabled()) {
                return;
            }
            for (IndexTrigger indexTrigger : triggersFor(entity.getClass())) {
                apply(indexRequests, indexTrigger, entity);
            }
        } catch (Exception ex) {
            log.warn("Could not enqueue search indexing for {}: {}", entity.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private void apply(SearchIndexRequests indexRequests, IndexTrigger indexTrigger, Object entity) {
        try {
            SearchIndexTrigger<?> trigger = indexTrigger.trigger();
            if (trigger.isFanOut()) {
                String key = trigger.fanOutKeyOf(entity);
                if (key != null) {
                    indexRequests.enqueueFanOut(indexTrigger.index(), key);
                }
            } else {
                UUID uuid = trigger.documentUuidOf(entity);
                if (uuid != null) {
                    indexRequests.enqueue(indexTrigger.index(), uuid);
                }
            }
        } catch (Exception ex) {
            log.warn("Search trigger for index {} failed on {}: {}", indexTrigger.index(),
                    entity.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private List<IndexTrigger> triggersFor(Class<?> entityClass) {
        return triggersByEntityClass.computeIfAbsent(entityClass, this::resolveTriggers);
    }

    private List<IndexTrigger> resolveTriggers(Class<?> entityClass) {
        List<IndexTrigger> matches = new ArrayList<>();
        sources.orderedStream().forEach(source -> {
            String index = source.definition().name();
            for (SearchIndexTrigger<?> trigger : source.triggers()) {
                // isAssignableFrom also matches Hibernate proxy subclasses of the entity.
                if (trigger.entityClass().isAssignableFrom(entityClass)) {
                    matches.add(new IndexTrigger(index, trigger));
                }
            }
        });
        return List.copyOf(matches);
    }

    private record IndexTrigger(String index, SearchIndexTrigger<?> trigger) {
    }
}
