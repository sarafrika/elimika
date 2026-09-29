package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;

/**
 * What a caller may find in the {@code instructors} index: platform admins see every profile,
 * everyone else only profiles an administrator has verified.
 */
public final class InstructorSearchScopes {

    private InstructorSearchScopes() {
    }

    public static SearchScope forCaller(boolean platformAdmin) {
        if (platformAdmin) {
            return SearchScope.unrestricted("platform-admin");
        }
        return SearchScope.of(SearchFilter.eq("admin_verified", true), "verified-instructors");
    }
}
