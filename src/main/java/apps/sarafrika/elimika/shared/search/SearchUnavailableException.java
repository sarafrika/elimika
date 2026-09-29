package apps.sarafrika.elimika.shared.search;

/**
 * Raised when the search engine cannot answer: search is disabled, the engine is unreachable, or it
 * refused the request. Callers that route a read through search catch this and fall back to the
 * database, so it must never be used for a caller's own validation errors.
 */
public class SearchUnavailableException extends RuntimeException {

    public SearchUnavailableException(String message) {
        super(message);
    }

    public SearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
