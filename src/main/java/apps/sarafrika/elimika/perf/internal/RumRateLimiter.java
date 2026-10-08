package apps.sarafrika.elimika.perf.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed one-minute window per caller: signed-in users by subject, anonymous visitors by client IP.
 * In memory and per instance; the map is bounded and evicts the least recently seen caller.
 */
@Component
public class RumRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_CALLERS = 20_000;

    private final int authenticatedLimit;
    private final int anonymousLimit;
    private final Clock clock;
    private final Map<String, Window> windows = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
            return size() > MAX_TRACKED_CALLERS;
        }
    };

    @Autowired
    public RumRateLimiter(@Value("${perf.rum.authenticated-batches-per-minute:60}") int authenticatedLimit,
                          @Value("${perf.rum.anonymous-batches-per-minute:12}") int anonymousLimit) {
        this(authenticatedLimit, anonymousLimit, Clock.systemUTC());
    }

    RumRateLimiter(int authenticatedLimit, int anonymousLimit, Clock clock) {
        this.authenticatedLimit = authenticatedLimit;
        this.anonymousLimit = anonymousLimit;
        this.clock = clock;
    }

    /** Counts one batch; false once the caller is over its limit for the current window. */
    public boolean tryAcquire(String userSubject, HttpServletRequest request) {
        boolean anonymous = !StringUtils.hasText(userSubject);
        String key = anonymous ? "ip:" + clientIp(request) : "user:" + userSubject;
        return tryAcquire(key, anonymous ? anonymousLimit : authenticatedLimit);
    }

    private synchronized boolean tryAcquire(String key, int limit) {
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current == null || !now.isBefore(current.startedAt.plus(WINDOW))) {
            windows.put(key, new Window(now));
            return limit > 0;
        }
        if (current.count >= limit) {
            return false;
        }
        current.count++;
        return true;
    }

    /** Proxy-set address first; only the last X-Forwarded-For hop is trusted, as clients control the rest. */
    static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (StringUtils.hasText(cloudflare)) {
            return cloudflare.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final Instant startedAt;
        private int count = 1;

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
