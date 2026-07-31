package apps.sarafrika.elimika.shared.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Memoises values that are constant for the lifetime of one HTTP request.
 * <p>
 * Authorization answers are derived from the caller's JWT, and neither the token nor the identity
 * behind it changes mid-request — yet every {@code @PreAuthorize} expression re-derives them from
 * scratch. A single guarded endpoint would otherwise re-resolve the principal and re-query the
 * user's domains once per domain tested, and a page that fans out to twenty of them multiplies that
 * again. Caching per request collapses all of it to one load.
 * <p>
 * Scoped to the request rather than to a TTL cache on purpose: an authorization decision must never
 * outlive the request that asked for it, so there is no window in which a revoked role is still
 * honoured.
 * <p>
 * Outside a request — schedulers, event listeners, async work — this degrades to computing the value
 * directly. Correctness never depends on the cache being present.
 */
@Component
public class RequestScopedCache {

    private static final String ATTRIBUTE = RequestScopedCache.class.getName();

    /**
     * Returns the memoised value for {@code key}, loading it once per request if absent.
     * {@code null} results are cached too, so a negative lookup is not repeated.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader) {
        Map<String, Object> memo = memo();
        if (memo == null) {
            return loader.get();
        }
        if (memo.containsKey(key)) {
            return (T) memo.get(key);
        }
        T value = loader.get();
        memo.put(key, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> memo() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Map<String, Object> memo =
                (Map<String, Object>) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (memo == null) {
            memo = new HashMap<>();
            attributes.setAttribute(ATTRIBUTE, memo, RequestAttributes.SCOPE_REQUEST);
        }
        return memo;
    }
}
