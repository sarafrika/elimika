package apps.sarafrika.elimika.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads the dashboard the caller is acting from off the request.
 * <p>
 * A header rather than a parameter, because the acting domain is not information about any one
 * resource: it qualifies every read the page makes, and threading it through the signature of each
 * service, each SPI and each {@code @PreAuthorize} expression that might one day consult it would
 * spread a cross-cutting fact across the whole call graph. It rides beside the bearer token, which
 * is where the rest of the caller's context already travels, and reaches these resolvers the same
 * way the principal does — through the request, not through an argument list.
 * <p>
 * Deliberately not a JWT claim: the token is minted at sign-in and outlives every dashboard switch
 * the user makes with it, so a claim would be stale the moment they changed pages. Deliberately not
 * a cookie either, since the API is a separate origin from the app and cookies would not survive
 * the trip.
 * <p>
 * Memoised with {@link RequestScopedCache} exactly as the domain lookups beside it are, so the
 * dozen resolvers a course page consults read the header once. Outside a request — schedulers,
 * event listeners, async work — there is no header and the answer is
 * {@link ActingDomain#UNSPECIFIED}, which caps nothing.
 */
@Component
@RequiredArgsConstructor
public class ActingDomainResolver {

    /**
     * The header clients send the active dashboard on.
     * <p>
     * Must be listed in the CORS allowed headers, or a browser's preflight will strip it and every
     * request will silently resolve to {@link ActingDomain#UNSPECIFIED}.
     */
    public static final String HEADER = "X-Acting-Domain";

    private static final String CACHE_KEY = "security.actingDomain";

    private final RequestScopedCache requestScopedCache;

    /**
     * @return the dashboard this request was made from, never null
     */
    public ActingDomain current() {
        return requestScopedCache.get(CACHE_KEY, ActingDomainResolver::readHeader);
    }

    private static ActingDomain readHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return ActingDomain.UNSPECIFIED;
        }
        return ActingDomain.fromHeader(servletAttributes.getRequest().getHeader(HEADER));
    }
}
