package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns a caller into the {@link SearchScope} of each course-module index. Every rule here mirrors
 * the SQL scope the same endpoint applies when it is served from the database, over the attributes
 * the documents carry for that purpose. The caller facts (admin flag, own identities, related course
 * UUIDs) are resolved by the service with the same per-request lookups its SQL scope uses.
 */
public final class CatalogueSearchScopes {

    private static final String PLATFORM_ADMIN = "platform-admin";

    private CatalogueSearchScopes() {
    }

    /**
     * Mirrors {@code CourseSpecificationBuilder#visibleTo}: the public catalogue (published,
     * admin-approved, active), the caller's own courses, and the courses they are enrolled in or
     * approved to teach. Shadow drafts are never indexed, so they need no clause.
     */
    public static SearchScope courses(boolean platformAdmin, UUID courseCreatorUuid, Collection<UUID> relatedCourseUuids) {
        if (platformAdmin) {
            return SearchScope.unrestricted(PLATFORM_ADMIN);
        }
        List<SearchFilter> visible = new ArrayList<>();
        visible.add(SearchFilter.eq(CourseSearchSource.IS_PUBLIC, true));
        if (courseCreatorUuid != null) {
            visible.add(SearchFilter.eq(CourseSearchSource.COURSE_CREATOR_UUID, courseCreatorUuid));
        }
        if (relatedCourseUuids != null && !relatedCourseUuids.isEmpty()) {
            visible.add(SearchFilter.in(CourseSearchSource.UUID_ATTRIBUTE, relatedCourseUuids));
        }
        return SearchScope.of(SearchFilter.or(visible),
                courseCreatorUuid == null ? "course-catalogue" : "course-catalogue+creator:" + courseCreatorUuid);
    }

    /**
     * Mirrors {@code TrainingProgramServiceImpl#visibleToCaller}: live programs (published, active,
     * admin-approved) plus the programs authored under any of the caller's own identities.
     */
    public static SearchScope programs(boolean platformAdmin, Set<UUID> ownIdentities) {
        if (platformAdmin) {
            return SearchScope.unrestricted(PLATFORM_ADMIN);
        }
        SearchFilter live = SearchFilter.eq(ProgramSearchSource.IS_PUBLIC, true);
        if (ownIdentities == null || ownIdentities.isEmpty()) {
            return SearchScope.of(live, "live-programs");
        }
        return SearchScope.of(SearchFilter.or(live, SearchFilter.in(ProgramSearchSource.COURSE_CREATOR_UUID, ownIdentities)),
                "live-programs+own");
    }

    /**
     * Mirrors {@code AssessmentRubricServiceImpl#visibleToCaller}: public rubrics plus the caller's
     * own, everything for a platform admin.
     */
    public static SearchScope rubrics(boolean platformAdmin, UUID courseCreatorUuid) {
        if (platformAdmin) {
            return SearchScope.unrestricted(PLATFORM_ADMIN);
        }
        SearchFilter shared = SearchFilter.eq(RubricSearchSource.IS_PUBLIC, true);
        if (courseCreatorUuid == null) {
            return SearchScope.of(shared, "public-rubrics");
        }
        return SearchScope.of(SearchFilter.or(shared, SearchFilter.eq(RubricSearchSource.COURSE_CREATOR_UUID, courseCreatorUuid)),
                "public-rubrics+creator:" + courseCreatorUuid);
    }

    /**
     * Mirrors the rubric discovery queries ({@code findPublicRubricsBySearchTerm}): public and active
     * rubrics only, for every caller.
     */
    public static SearchScope publicRubrics() {
        return SearchScope.of(SearchFilter.and(
                SearchFilter.eq(RubricSearchSource.IS_PUBLIC, true),
                SearchFilter.eq(RubricSearchSource.IS_ACTIVE, true)), "public-active-rubrics");
    }
}
