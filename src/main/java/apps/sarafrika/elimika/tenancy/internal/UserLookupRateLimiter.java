package apps.sarafrika.elimika.tenancy.internal;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-caller fixed-window limit on user-number lookups, so the route cannot be used to enumerate
 * accounts. In memory and per instance (there is no shared cache); the map is bounded and evicts
 * the least recently seen caller once full.
 */
@Component
public class UserLookupRateLimiter {

    public static final int DEFAULT_LIMIT = 20;
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);
    private static final int DEFAULT_MAX_CALLERS = 10_000;

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Window> windows;

    public UserLookupRateLimiter() {
        this(DEFAULT_LIMIT, DEFAULT_WINDOW, DEFAULT_MAX_CALLERS, Clock.systemUTC());
    }

    public UserLookupRateLimiter(int limit, Duration window, int maxCallers, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
        this.windows = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
                return size() > maxCallers;
            }
        };
    }

    /** Counts one lookup for the caller; false once they are over the limit for the current window. */
    public synchronized boolean tryAcquire(String callerKey) {
        Instant now = clock.instant();
        Window current = windows.get(callerKey);
        if (current == null || !now.isBefore(current.startedAt.plus(window))) {
            windows.put(callerKey, new Window(now));
            return true;
        }
        if (current.count >= limit) {
            return false;
        }
        current.count++;
        return true;
    }

    private static final class Window {
        private final Instant startedAt;
        private int count = 1;

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
