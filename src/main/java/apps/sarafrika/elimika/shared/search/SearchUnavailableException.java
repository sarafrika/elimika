package apps.sarafrika.elimika.shared.search;

/**
 * Raised when the search engine cannot answer: search is disabled, an index's reads are not enabled,
 * the engine is unreachable, or it refused the request. Free text ({@code q}) is served only by
 * search, so this propagates to the global handler as a 503 ("Search is unavailable"); there is no
 * database fallback. It must never be used for a caller's own validation errors (those are 400s).
 */
public class SearchUnavailableException extends RuntimeException {

    public SearchUnavailableException(String message) {
        super(message);
    }

    public SearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
