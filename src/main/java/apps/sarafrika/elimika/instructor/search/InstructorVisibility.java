package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * The one rule for which instructor profiles a caller may list, applied identically by the database
 * listing ({@code GET /instructors}, {@code /instructors/search}) and the {@code instructors} index.
 * <ul>
 *     <li>A platform admin lists every profile, including the verification queue.</li>
 *     <li>Everyone else <em>discovers</em> - browses, or searches by text or attributes - only
 *     admin-verified profiles, plus their own profile whatever its state, so an instructor awaiting
 *     verification still finds themselves.</li>
 *     <li>An <em>exact identity lookup</em> - a request pinned to {@code uuid}/{@code uuid_in} or
 *     {@code user_uuid}/{@code user_uuid_in} - resolves the named profiles whatever their state. The
 *     caller already holds those identifiers (from a class, an application, a booking), and
 *     {@code GET /instructors/{uuid}} already returns any single profile, so this reveals nothing new;
 *     it is what keeps organisation, course-creator and booking screens able to show the name of an
 *     instructor who is not verified yet.</li>
 * </ul>
 * The instructor module may depend only on {@code shared}, so organisation affiliations are not
 * reachable here; the identity lookup covers the screens that need them.
 */
@Component
@RequiredArgsConstructor
public class InstructorVisibility {

    private static final Set<String> IDENTITY_FIELDS = Set.of("uuid", "useruuid");
    private static final Set<String> IDENTITY_OPERATIONS = Set.of("eq", "in");
    private static final Set<String> OPERATIONS = Set.of(
            "eq", "noteq", "in", "notin", "gt", "gte", "lt", "lte", "between");

    private final DomainSecurityService domainSecurityService;

    /** The facts the rule needs about the current caller, looked up once. */
    public record Caller(boolean platformAdmin, UUID ownInstructorUuid) {
    }

    public Caller currentCaller() {
        if (domainSecurityService.isPlatformAdmin()) {
            return new Caller(true, null);
        }
        return new Caller(false, domainSecurityService.getCurrentInstructorUuid());
    }

    /**
     * The database boundary for a listing with these params, or {@code null} when the caller may see
     * every row (a platform admin, or an exact identity lookup).
     */
    public Specification<Instructor> databaseScope(Caller caller, Map<String, String> params) {
        if (caller.platformAdmin() || isIdentityLookup(params)) {
            return null;
        }
        UUID own = caller.ownInstructorUuid();
        return (root, query, cb) -> own == null
                ? cb.isTrue(root.get("adminVerified"))
                : cb.or(cb.isTrue(root.get("adminVerified")), cb.equal(root.get("uuid"), own));
    }

    /** The same rule over the {@code instructors} index. */
    public SearchScope searchScope(Caller caller, Map<String, String> params) {
        if (caller.platformAdmin()) {
            return SearchScope.unrestricted("platform-admin");
        }
        return InstructorSearchScopes.forCaller(caller.ownInstructorUuid(), pinnedUuids(params));
    }

    /** Whether the request names the profiles it wants by {@code uuid} or {@code user_uuid}. */
    static boolean isIdentityLookup(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        return params.entrySet().stream().anyMatch(entry -> identityKey(entry.getKey()) != null
                && entry.getValue() != null && !entry.getValue().isBlank());
    }

    /** The document UUIDs a request pins with {@code uuid}, {@code uuid_eq} or {@code uuid_in}. */
    static Set<UUID> pinnedUuids(Map<String, String> params) {
        Set<UUID> pinned = new LinkedHashSet<>();
        if (params == null) {
            return pinned;
        }
        params.forEach((key, value) -> {
            if ("uuid".equals(identityKey(key)) && value != null) {
                Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(v -> {
                    try {
                        pinned.add(UUID.fromString(v));
                    } catch (IllegalArgumentException ignored) {
                        // Not a UUID: it matches nothing, so it pins nothing.
                    }
                });
            }
        });
        return pinned;
    }

    /** {@code uuid} or {@code useruuid} for an identity-pinning key, else {@code null}. */
    private static String identityKey(String key) {
        if (key == null) {
            return null;
        }
        String field = key;
        String operation = "eq";
        int lastUnderscore = key.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < key.length() - 1) {
            String suffix = key.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT);
            if (OPERATIONS.contains(suffix)) {
                field = key.substring(0, lastUnderscore);
                operation = suffix;
            }
        }
        String normalised = field.replace("_", "").toLowerCase(Locale.ROOT);
        return IDENTITY_FIELDS.contains(normalised) && IDENTITY_OPERATIONS.contains(operation) ? normalised : null;
    }

    static List<SearchFilter> ownOrPinned(UUID ownInstructorUuid, Set<UUID> pinnedUuids) {
        List<SearchFilter> filters = new ArrayList<>();
        if (ownInstructorUuid != null) {
            filters.add(SearchFilter.eq("uuid", ownInstructorUuid));
        }
        if (pinnedUuids != null && !pinnedUuids.isEmpty()) {
            filters.add(SearchFilter.in("uuid", pinnedUuids));
        }
        return filters;
    }
}
