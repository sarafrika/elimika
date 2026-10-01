package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code course_content} in global search: lessons, content items, quizzes and assignments of the
 * courses the caller manages, and the published course material of the courses they are enrolled in.
 * <p>
 * Hidden from anonymous callers and from anyone with neither (the scope is empty). Because global
 * search builds results from stored fields, every page is also re-checked in SQL ({@link #recheck}),
 * so a revoked enrolment or an unpublished lesson never surfaces while the index catches up.
 */
@Component
class CourseContentGlobalSearchProvider implements GlobalSearchProvider {

    private final CourseContentEntitlement entitlement;

    CourseContentGlobalSearchProvider(CourseContentEntitlement entitlement) {
        this.entitlement = entitlement;
    }

    @Override
    public String type() {
        return CourseContentSearchSource.INDEX;
    }

    @Override
    public SearchIndexDefinition definition() {
        return CourseContentSearchSource.DEFINITION;
    }

    @Override
    public Optional<SearchScope> scopeForCurrentCaller() {
        return entitlement.scope();
    }

    /**
     * Title is the item's; subtitle is "Lesson {n} · {course}"; the context carries the course and
     * lesson so the palette can open the hit inside its lesson.
     */
    @Override
    public GlobalSearchHit toHit(SearchHit hit) {
        Map<String, Object> document = hit.document();
        Object lessonNumber = document == null ? null : document.get("lesson_number");
        String courseName = GlobalSearchHit.text(document, "course_name");
        String subtitle;
        if (lessonNumber == null) {
            subtitle = courseName;
        } else {
            subtitle = "Lesson " + lessonNumber + (courseName == null ? "" : " · " + courseName);
        }
        return new GlobalSearchHit(type(), hit.uuid(), GlobalSearchHit.text(document, "title"), subtitle, null,
                CourseContentSearchService.highlight(hit))
                .withContext(new GlobalSearchHit.Context(
                        GlobalSearchHit.uuid(document, CourseContentSearchSource.COURSE_UUID),
                        GlobalSearchHit.uuid(document, "lesson_uuid")));
    }

    @Override
    public List<GlobalSearchHit> recheck(List<GlobalSearchHit> hits) {
        return entitlement.retainVisible(hits, GlobalSearchHit::uuid);
    }
}
