package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.internal.ClassListingVisibility;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code classes} index boundary for a caller, built from the same per-request lookups as the
 * SQL listing ({@link ClassListingVisibility.Scope#toSpecification()}): platform admins see every
 * class; everyone else sees active PUBLIC classes, plus the classes of organisations they staff, the
 * classes they teach and the classes they are enrolled in.
 */
public final class ClassSearchScopes {

    private ClassSearchScopes() {
    }

    public static SearchScope forListing(ClassListingVisibility.Scope scope) {
        if (scope.everything()) {
            return SearchScope.unrestricted("platform-admin");
        }
        List<SearchFilter> visible = new ArrayList<>();
        visible.add(SearchFilter.and(
                SearchFilter.eq("is_active", true),
                SearchFilter.eq("class_visibility", ClassVisibility.PUBLIC.name())));
        if (!scope.staffedOrganisations().isEmpty()) {
            visible.add(SearchFilter.in("organisation_uuid", scope.staffedOrganisations()));
        }
        if (scope.instructorUuid() != null) {
            visible.add(SearchFilter.eq("default_instructor_uuid", scope.instructorUuid()));
        }
        if (!scope.enrolledClasses().isEmpty()) {
            visible.add(SearchFilter.in("uuid", scope.enrolledClasses()));
        }
        String label = "public+staffed:" + scope.staffedOrganisations().size()
                + "+teaches:" + (scope.instructorUuid() != null)
                + "+enrolled:" + scope.enrolledClasses().size();
        return SearchScope.of(SearchFilter.or(visible), label);
    }
}
