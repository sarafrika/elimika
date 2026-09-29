package apps.sarafrika.elimika.shared.search;

import java.util.Objects;

/**
 * The visibility boundary of a search - what the caller is allowed to see, expressed as a filter.
 * <p>
 * Every {@link SearchRequest} carries one and there is no default. The engine ANDs it with whatever
 * the caller asked for, so a user filter can narrow a scope but never widen it. Owning modules build
 * scopes in one place (a scope factory next to their document source) from the same rules their
 * database queries already apply.
 * <p>
 * {@link #unrestricted(String)} is the only way to search without a boundary, and it has to be
 * written out by name - it is meant for platform administrators and internal jobs.
 *
 * @param filter the boundary; {@code null} only for an unrestricted scope
 * @param label  a short description for logs, e.g. {@code "public-courses"} or {@code "org:<uuid>"}
 */
public record SearchScope(SearchFilter filter, String label) {

    public SearchScope {
        Objects.requireNonNull(label, "label");
    }

    /** A scope limited by {@code filter}. */
    public static SearchScope of(SearchFilter filter, String label) {
        return new SearchScope(Objects.requireNonNull(filter, "filter"), label);
    }

    /** No boundary at all. Use only where the caller may genuinely see every document. */
    public static SearchScope unrestricted(String label) {
        return new SearchScope(null, label);
    }

    public boolean isUnrestricted() {
        return filter == null;
    }
}
