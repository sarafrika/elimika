package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.security.LearnerContentAccess;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchScope;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Who may see which {@code course_content} documents, for both the in-course search and global search.
 * <p>
 * The rule mirrors the SQL entitlement the material endpoints already apply:
 * <ul>
 *     <li>a platform admin (on a dashboard that may act as one) sees everything;</li>
 *     <li>staff see every item of the courses they manage ({@link CourseSecuritySpi#manageableCourseUuids()}:
 *     authored, approved to train individually or through a teaching organisation, or via a programme),
 *     drafts included, unless the request comes from a learner's dashboard ({@link CourseFootingCap});</li>
 *     <li>a learner sees the published, course-scoped items of the courses they are enrolled in
 *     ({@link CourseSecuritySpi#enrolledCourseUuids()}: active or completed enrolments). Class-scoped
 *     quizzes and assignments stay hidden in v1, and guardians get nothing.</li>
 * </ul>
 * The engine narrows with {@link #scope()}; {@link #retainVisible} re-checks the hits in SQL, so a
 * lagging index (a revoked enrolment, an unpublished lesson, a deleted item) never widens access.
 */
@Component
public class CourseContentEntitlement {

    private static final String RECHECK = """
            SELECT x.uuid, x.course_uuid, x.published, x.class_scoped FROM (
                SELECT l.uuid, l.course_uuid, (LOWER(l.status) = 'published' AND l.active) AS published,
                       false AS class_scoped
                FROM lessons l WHERE l.uuid IN (:uuids)
                UNION ALL
                SELECT x.uuid, l.course_uuid, (LOWER(l.status) = 'published' AND l.active), false
                FROM lesson_contents x JOIN lessons l ON l.uuid = x.lesson_uuid WHERE x.uuid IN (:uuids)
                UNION ALL
                SELECT x.uuid, l.course_uuid,
                       (LOWER(x.status) = 'published' AND x.active AND LOWER(l.status) = 'published' AND l.active),
                       UPPER(x.scope) = 'CLASS_CLONE'
                FROM quizzes x JOIN lessons l ON l.uuid = x.lesson_uuid WHERE x.uuid IN (:uuids)
                UNION ALL
                SELECT x.uuid, l.course_uuid,
                       (COALESCE(x.is_published, false) AND LOWER(l.status) = 'published' AND l.active),
                       UPPER(x.scope) = 'CLASS_CLONE'
                FROM assignments x JOIN lessons l ON l.uuid = x.lesson_uuid WHERE x.uuid IN (:uuids)
            ) x JOIN courses c ON c.uuid = x.course_uuid
            WHERE c.parent_course_uuid IS NULL
            """;

    private final CourseSecuritySpi courseSecurity;
    private final LearnerContentAccess learnerContentAccess;
    private final CourseFootingCap courseFootingCap;
    private final NamedParameterJdbcTemplate jdbc;

    public CourseContentEntitlement(CourseSecuritySpi courseSecurity, LearnerContentAccess learnerContentAccess,
                                    CourseFootingCap courseFootingCap, NamedParameterJdbcTemplate jdbc) {
        this.courseSecurity = courseSecurity;
        this.learnerContentAccess = learnerContentAccess;
        this.courseFootingCap = courseFootingCap;
        this.jdbc = jdbc;
    }

    /** The current caller's facts, resolved with the per-request memoised lookups the SQL paths use. */
    Caller currentCaller() {
        if (learnerContentAccess.isPlatformAdmin()) {
            return new Caller(true, Set.of(), Set.of());
        }
        Set<UUID> managed = courseFootingCap.permitsAnyStaffFooting() ? courseSecurity.manageableCourseUuids() : Set.of();
        return new Caller(false, managed, courseSecurity.enrolledCourseUuids());
    }

    /** The caller's boundary over the index, or empty when they may see no course content at all. */
    Optional<SearchScope> scope() {
        return currentCaller().scope();
    }

    /**
     * The items the caller may still see, in the given order: each is looked up in one batch query and
     * kept only while it exists in a root course and the caller's entitlement covers it now.
     */
    <T> List<T> retainVisible(List<T> items, Function<T, UUID> uuidOf) {
        if (items.isEmpty()) {
            return items;
        }
        Caller caller = currentCaller();
        List<UUID> uuids = items.stream().map(uuidOf).toList();
        Map<UUID, Boolean> visible = new HashMap<>();
        jdbc.query(RECHECK, new MapSqlParameterSource("uuids", uuids), rs -> {
            visible.put(SearchRows.uuid(rs, "uuid"), caller.sees(SearchRows.uuid(rs, "course_uuid"),
                    SearchRows.flag(rs, "published"), SearchRows.flag(rs, "class_scoped")));
        });
        List<T> kept = new ArrayList<>(items.size());
        for (T item : items) {
            if (Boolean.TRUE.equals(visible.get(uuidOf.apply(item)))) {
                kept.add(item);
            }
        }
        return kept;
    }

    /**
     * @param admin    a platform admin acting as one
     * @param managed  courses whose every item the caller sees
     * @param enrolled courses whose published, course-scoped items the caller sees
     */
    record Caller(boolean admin, Set<UUID> managed, Set<UUID> enrolled) {

        boolean sees(UUID courseUuid, boolean published, boolean classScoped) {
            return admin || managed.contains(courseUuid)
                    || (enrolled.contains(courseUuid) && published && !classScoped);
        }

        Optional<SearchScope> scope() {
            if (admin) {
                return Optional.of(SearchScope.unrestricted("platform-admin"));
            }
            List<SearchFilter> visible = new ArrayList<>();
            if (!managed.isEmpty()) {
                visible.add(SearchFilter.in(CourseContentSearchSource.COURSE_UUID, managed));
            }
            Collection<UUID> learnerCourses = enrolled.stream().filter(course -> !managed.contains(course)).toList();
            if (!learnerCourses.isEmpty()) {
                visible.add(SearchFilter.and(
                        SearchFilter.in(CourseContentSearchSource.COURSE_UUID, learnerCourses),
                        SearchFilter.eq(CourseContentSearchSource.PUBLISHED, true),
                        SearchFilter.eq(CourseContentSearchSource.SCOPE, CourseContentSearchSource.SCOPE_COURSE)));
            }
            if (visible.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(SearchScope.of(SearchFilter.or(visible), "course-content:managed+enrolled"));
        }
    }
}
