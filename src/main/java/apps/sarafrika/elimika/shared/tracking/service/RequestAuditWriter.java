package apps.sarafrika.elimika.shared.tracking.service;

import apps.sarafrika.elimika.shared.tracking.model.RequestAuditEntry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Persists request audit entries off the request path.
 *
 * <p>Request threads {@link #offer(RequestAuditEntry) offer} entries to a bounded in-memory queue and never touch
 * the database. A single daemon thread drains the queue and writes it with one JDBC batch insert per
 * {@code batchSize} rows or per {@code flushInterval}, whichever comes first, so audit costs at most one
 * connection checkout per second instead of one per request.
 *
 * <p>When the queue is full the entry is dropped, counted in {@code elimika.audit.dropped} and reported by a
 * rate-limited warning: losing audit rows under overload is preferred over stalling or failing user requests.
 * A failed batch is logged and counted in {@code elimika.audit.failed}; it is not retried.
 *
 * <p>{@code user_uuid} is resolved here from the Keycloak subject with one {@code IN} query per batch and a small
 * writer-local cache, so a signed-in request no longer pays for user lookups. On shutdown the remaining queue is
 * flushed; the lifecycle phase is below the web server's, so this stops after in-flight requests have drained.
 */
@Component
@Slf4j
public class RequestAuditWriter implements SmartLifecycle {

    static final String INSERT_SQL = """
            INSERT INTO request_audit_log (
                request_id, http_method, request_uri, query_string, ip_address, remote_host, user_agent, referer,
                session_id, header_snapshot, response_status, processing_time_ms, authentication_name, user_uuid,
                user_email, user_full_name, user_domains, keycloak_id, created_date, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final int USER_UUID_CACHE_SIZE = 5_000;
    private static final long DROP_WARN_INTERVAL_MS = 60_000;
    private static final long STOP_TIMEOUT_MS = 15_000;

    private final JdbcTemplate jdbcTemplate;
    private final BlockingQueue<RequestAuditEntry> queue;
    private final int batchSize;
    private final long flushIntervalMs;
    private final Counter droppedCounter;
    private final Counter failedCounter;
    private final Counter writtenCounter;
    private final AtomicLong lastDropWarnAt = new AtomicLong();
    private final AtomicLong droppedSinceWarn = new AtomicLong();

    /** Keycloak id to user uuid; the mapping never changes, so entries only leave by LRU eviction. */
    private final Map<String, UUID> userUuidCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
                    return size() > USER_UUID_CACHE_SIZE;
                }
            });

    private volatile boolean running;
    private volatile Thread worker;

    @Autowired
    public RequestAuditWriter(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry,
            @Value("${app.audit.queue-capacity:10000}") int queueCapacity,
            @Value("${app.audit.batch-size:500}") int batchSize,
            @Value("${app.audit.flush-interval-ms:1000}") long flushIntervalMs
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.droppedCounter = Counter.builder("elimika.audit.dropped")
                .description("Request audit entries dropped because the in-memory queue was full")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("elimika.audit.failed")
                .description("Request audit entries lost because their batch insert failed")
                .register(meterRegistry);
        this.writtenCounter = Counter.builder("elimika.audit.written")
                .description("Request audit entries written to request_audit_log")
                .register(meterRegistry);
        Gauge.builder("elimika.audit.queue.size", queue, BlockingQueue::size)
                .description("Request audit entries waiting to be written")
                .register(meterRegistry);
    }

    /**
     * Enqueues an entry without blocking. Returns {@code false}, and counts a drop, when the queue is full.
     */
    public boolean offer(RequestAuditEntry entry) {
        if (queue.offer(entry)) {
            return true;
        }
        droppedCounter.increment();
        droppedSinceWarn.incrementAndGet();
        long now = System.currentTimeMillis();
        long last = lastDropWarnAt.get();
        if (now - last >= DROP_WARN_INTERVAL_MS && lastDropWarnAt.compareAndSet(last, now)) {
            log.warn("Request audit queue is full; dropped {} entries in the last {}s (total {})",
                    droppedSinceWarn.getAndSet(0), DROP_WARN_INTERVAL_MS / 1000, (long) droppedCounter.count());
        }
        return false;
    }

    int pendingCount() {
        return queue.size();
    }

    /**
     * Drains up to one batch from the queue and writes it. Returns the number of entries taken.
     */
    int flushOnce() {
        List<RequestAuditEntry> batch = new ArrayList<>(Math.min(batchSize, queue.size()));
        queue.drainTo(batch, batchSize);
        write(batch);
        return batch.size();
    }

    /** Writes everything currently queued, one batch at a time. */
    void flushAll() {
        while (flushOnce() > 0) {
            // keep draining
        }
    }

    private void runLoop() {
        List<RequestAuditEntry> batch = new ArrayList<>(batchSize);
        long deadline = System.currentTimeMillis() + flushIntervalMs;
        while (running) {
            try {
                long wait = Math.max(0, deadline - System.currentTimeMillis());
                RequestAuditEntry head = queue.poll(wait, TimeUnit.MILLISECONDS);
                if (head != null) {
                    batch.add(head);
                    queue.drainTo(batch, batchSize - batch.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long now = System.currentTimeMillis();
            if (batch.size() >= batchSize || now >= deadline) {
                write(batch);
                batch = new ArrayList<>(batchSize);
                deadline = now + flushIntervalMs;
            }
        }
        write(batch);
        flushAll();
    }

    private void write(List<RequestAuditEntry> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            insert(batch, resolveUserUuids(batch));
            writtenCounter.increment(batch.size());
        } catch (DataIntegrityViolationException stale) {
            // A cached user uuid can outlive its user row; re-resolve, and keep the rows without it if that fails.
            userUuidCache.clear();
            try {
                insert(batch, resolveUserUuids(batch));
            } catch (DataIntegrityViolationException stillStale) {
                insert(batch, Map.of());
            }
            writtenCounter.increment(batch.size());
        } catch (Exception ex) {
            failedCounter.increment(batch.size());
            log.error("Failed to write {} request audit entries", batch.size(), ex);
        }
    }

    private void insert(List<RequestAuditEntry> batch, Map<String, UUID> userUuids) {
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                bind(ps, batch.get(i), userUuids);
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }

    private Map<String, UUID> resolveUserUuids(List<RequestAuditEntry> batch) {
        Set<String> keycloakIds = new LinkedHashSet<>();
        for (RequestAuditEntry entry : batch) {
            if (entry.keycloakId() != null) {
                keycloakIds.add(entry.keycloakId());
            }
        }
        if (keycloakIds.isEmpty()) {
            return Map.of();
        }
        Map<String, UUID> resolved = new HashMap<>();
        List<String> missing = new ArrayList<>();
        for (String keycloakId : keycloakIds) {
            UUID cached = userUuidCache.get(keycloakId);
            if (cached != null) {
                resolved.put(keycloakId, cached);
            } else {
                missing.add(keycloakId);
            }
        }
        if (!missing.isEmpty()) {
            try {
                String placeholders = String.join(",", Collections.nCopies(missing.size(), "?"));
                jdbcTemplate.query(
                        "SELECT keycloak_id, uuid FROM users WHERE keycloak_id IN (" + placeholders + ")",
                        rs -> {
                            String keycloakId = rs.getString(1);
                            UUID uuid = rs.getObject(2, UUID.class);
                            resolved.put(keycloakId, uuid);
                            userUuidCache.put(keycloakId, uuid);
                        },
                        missing.toArray());
            } catch (Exception ex) {
                // Rows are still worth keeping without the actor uuid; email and keycloak id identify the caller.
                log.warn("Unable to resolve user uuids for request audit batch: {}", ex.getMessage());
            }
        }
        return resolved;
    }

    private static void bind(PreparedStatement ps, RequestAuditEntry e, Map<String, UUID> userUuids)
            throws SQLException {
        ps.setString(1, e.requestId());
        ps.setString(2, e.httpMethod());
        ps.setString(3, e.requestUri());
        ps.setString(4, e.queryString());
        ps.setString(5, e.ipAddress());
        ps.setString(6, e.remoteHost());
        ps.setString(7, e.userAgent());
        ps.setString(8, e.referer());
        ps.setString(9, e.sessionId());
        ps.setString(10, e.headerSnapshot());
        if (e.responseStatus() == null) {
            ps.setNull(11, Types.INTEGER);
        } else {
            ps.setInt(11, e.responseStatus());
        }
        if (e.processingTimeMs() == null) {
            ps.setNull(12, Types.BIGINT);
        } else {
            ps.setLong(12, e.processingTimeMs());
        }
        ps.setString(13, e.authenticationName());
        UUID userUuid = e.keycloakId() == null ? null : userUuids.get(e.keycloakId());
        if (userUuid == null) {
            ps.setNull(14, Types.OTHER);
        } else {
            ps.setObject(14, userUuid);
        }
        ps.setString(15, e.userEmail());
        ps.setString(16, e.userFullName());
        ps.setString(17, e.userDomains());
        ps.setString(18, e.keycloakId());
        ps.setTimestamp(19, Timestamp.valueOf(e.createdDate()));
        ps.setString(20, e.createdBy());
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(this::runLoop, "request-audit-writer");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
    }

    @Override
    public synchronized void stop() {
        running = false;
        Thread thread = worker;
        worker = null;
        if (thread == null) {
            return;
        }
        try {
            // The loop wakes within one flush interval, writes what is left and exits.
            thread.join(STOP_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            log.warn("Request audit writer did not finish within {}ms; {} entries may be lost",
                    STOP_TIMEOUT_MS, queue.size());
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Below the web server's stop phase, so the queue is flushed after in-flight requests finish and before the
     * DataSource is closed.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 4096;
    }
}
