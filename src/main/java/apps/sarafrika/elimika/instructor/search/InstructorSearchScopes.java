package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What a caller may find in the {@code instructors} index - the index side of
 * {@link InstructorVisibility}: platform admins see every profile; everyone else verified profiles,
 * their own profile, and any profile the request pins by UUID.
 */
public final class InstructorSearchScopes {

    private InstructorSearchScopes() {
    }

    public static SearchScope forCaller(boolean platformAdmin) {
        if (platformAdmin) {
            return SearchScope.unrestricted("platform-admin");
        }
        return forCaller(null, Set.of());
    }

    /** A non-admin caller's boundary: verified, or their own profile, or pinned by UUID. */
    public static SearchScope forCaller(UUID ownInstructorUuid, Set<UUID> pinnedUuids) {
        List<SearchFilter> visible = new ArrayList<>();
        visible.add(SearchFilter.eq("admin_verified", true));
        visible.addAll(InstructorVisibility.ownOrPinned(ownInstructorUuid, pinnedUuids));
        String label = "verified-instructors"
                + (ownInstructorUuid != null ? "+own" : "")
                + (pinnedUuids != null && !pinnedUuids.isEmpty() ? "+pinned:" + pinnedUuids.size() : "");
        return SearchScope.of(SearchFilter.or(visible), label);
    }
}
