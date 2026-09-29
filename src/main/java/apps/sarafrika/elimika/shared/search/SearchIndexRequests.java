package apps.sarafrika.elimika.shared.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Collects the search documents a transaction touched and hands them to the search module as one
 * {@link SearchIndexRequested} per index.
 * <p>
 * The collection is bound to the current transaction. On first use a {@link TransactionSynchronization}
 * is registered; just before commit it publishes the collected requests through the
 * {@link ApplicationEventPublisher}. Publishing <em>before</em> commit is the point: Spring
 * Modulith's event publication registry writes its row in the same transaction as the business
 * change, so an indexing request cannot be lost to a crash between commit and publish, and a
 * transaction that rolls back publishes nothing.
 * <p>
 * JPA only runs the post-insert/update/delete callbacks that feed this collector when the session is
 * flushed, and Spring's JPA transaction manager flushes <em>after</em> the before-commit callbacks -
 * so a change first flushed by the commit would reach this collector too late, and one made in a
 * transaction that had not enqueued anything yet would never register a synchronization at all.
 * Two things close that gap:
 * <ul>
 *     <li>this bean is a {@link TransactionExecutionListener}, which Spring Boot registers with the
 *     transaction manager, so every new read-write transaction gets the synchronization up front;</li>
 *     <li>the synchronization flushes the transaction's entity manager itself before publishing, so
 *     the entity callbacks have all run by then. The commit's own flush then finds nothing left but
 *     the publication rows.</li>
 * </ul>
 * <p>
 * Outside a transaction requests are published immediately. With {@code search.enabled=false} this
 * is a no-op, so nothing reaches the publication registry.
 */
@Slf4j
@Component
public class SearchIndexRequests implements TransactionExecutionListener {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<EntityManagerFactory> entityManagerFactories;
    private final boolean enabled;

    public SearchIndexRequests(
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<EntityManagerFactory> entityManagerFactories,
            @Value("${search.enabled:false}") boolean enabled
    ) {
        this.eventPublisher = eventPublisher;
        this.entityManagerFactories = entityManagerFactories;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Re-sync these documents of {@code index} once the current transaction commits. */
    public void enqueue(String index, Collection<UUID> uuids) {
        if (!enabled || uuids == null || uuids.isEmpty()) {
            return;
        }
        Objects.requireNonNull(index, "index");
        Pending pending = currentPending();
        if (pending == null) {
            eventPublisher.publishEvent(new SearchIndexRequested(index, Set.copyOf(uuids), Set.of()));
            return;
        }
        pending.add(index, uuids, null);
    }

    public void enqueue(String index, UUID uuid) {
        if (uuid != null) {
            enqueue(index, Set.of(uuid));
        }
    }

    /** Re-sync every document the index's source resolves {@code key} to, once the transaction commits. */
    public void enqueueFanOut(String index, String key) {
        if (!enabled || key == null || key.isBlank()) {
            return;
        }
        Objects.requireNonNull(index, "index");
        Pending pending = currentPending();
        if (pending == null) {
            eventPublisher.publishEvent(new SearchIndexRequested(index, Set.of(), Set.of(key)));
            return;
        }
        pending.add(index, null, key);
    }

    /**
     * Registers the collector with every new read-write transaction, so changes that are first
     * flushed at commit are still collected and published.
     */
    @Override
    public void afterBegin(TransactionExecution transaction, Throwable beginFailure) {
        if (!enabled || beginFailure != null || transaction.isReadOnly()) {
            return;
        }
        currentPending();
    }

    /** The collector for the current transaction, registering it on first use; null outside one. */
    private Pending currentPending() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }
        Pending pending = (Pending) TransactionSynchronizationManager.getResource(this);
        if (pending != null) {
            if (pending.published) {
                // Only reachable when something is flushed after our before-commit flush, e.g. by a
                // synchronization that runs later. The reconciler catches what this drops.
                log.warn("Search index request arrived after this transaction's requests were published; "
                        + "it will be picked up by reconciliation");
                return new Pending();
            }
            return pending;
        }
        Pending created = new Pending();
        TransactionSynchronizationManager.bindResource(this, created);
        TransactionSynchronizationManager.registerSynchronization(new PublishBeforeCommit(created));
        return created;
    }

    private final class PublishBeforeCommit implements TransactionSynchronization {

        private final Pending pending;

        private PublishBeforeCommit(Pending pending) {
            this.pending = pending;
        }

        @Override
        public int getOrder() {
            // Last, so changes made by other before-commit callbacks are collected too.
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            if (!readOnly) {
                flushEntityManager();
            }
            pending.published = true;
            pending.byIndex.forEach((index, request) -> eventPublisher.publishEvent(
                    new SearchIndexRequested(index, request.uuids, request.fanOutKeys)));
        }

        @Override
        public void afterCompletion(int status) {
            if (TransactionSynchronizationManager.getResource(SearchIndexRequests.this) == pending) {
                TransactionSynchronizationManager.unbindResource(SearchIndexRequests.this);
            }
        }
    }

    private void flushEntityManager() {
        EntityManagerFactory entityManagerFactory = entityManagerFactories.getIfAvailable();
        if (entityManagerFactory == null) {
            return;
        }
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (entityManager != null && entityManager.isJoinedToTransaction()) {
            entityManager.flush();
        }
    }

    private static final class Pending {
        private final Map<String, IndexRequest> byIndex = new LinkedHashMap<>();
        private boolean published;

        private void add(String index, Collection<UUID> uuids, String fanOutKey) {
            IndexRequest request = byIndex.computeIfAbsent(index, ignored -> new IndexRequest());
            if (uuids != null) {
                uuids.stream().filter(Objects::nonNull).forEach(request.uuids::add);
            }
            if (fanOutKey != null) {
                request.fanOutKeys.add(fanOutKey);
            }
        }
    }

    private static final class IndexRequest {
        private final Set<UUID> uuids = new LinkedHashSet<>();
        private final Set<String> fanOutKeys = new LinkedHashSet<>();
    }
}
