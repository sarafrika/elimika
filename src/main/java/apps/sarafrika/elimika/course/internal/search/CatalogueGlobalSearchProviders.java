package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The course module's three indexes in global search. Each scope is the one the module's own
 * {@code q} listing uses ({@link CatalogueSearchScopes}), built from the same per-request lookups, so
 * an anonymous caller - who has no creator profile, enrolments or own identities - gets the public
 * catalogue only.
 */
public final class CatalogueGlobalSearchProviders {

    private CatalogueGlobalSearchProviders() {
    }

    /** {@code courses}: the public catalogue plus the caller's own and related courses. */
    @Component
    static class Courses implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;
        private final CourseSecuritySpi courseSecurity;

        Courses(DomainSecurityService domainSecurityService, CourseSecuritySpi courseSecurity) {
            this.domainSecurityService = domainSecurityService;
            this.courseSecurity = courseSecurity;
        }

        @Override
        public String type() {
            return CourseSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return CourseSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            if (domainSecurityService.isPlatformAdmin()) {
                return Optional.of(CatalogueSearchScopes.courses(true, null, Set.of()));
            }
            Set<UUID> related = new HashSet<>(courseSecurity.enrolledCourseUuids());
            related.addAll(courseSecurity.manageableCourseUuids());
            return Optional.of(CatalogueSearchScopes.courses(false,
                    domainSecurityService.getCurrentCourseCreatorUuid(), related));
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "name"),
                    GlobalSearchHit.text(document, "creator_name"),
                    FileUrlResolver.publicUrl(GlobalSearchHit.text(document, "thumbnail_url")),
                    GlobalSearchHit.highlight(hit, "name", "category_names", "creator_name", "description"));
        }
    }

    /** {@code programs}: live programs plus those authored under the caller's own identities. */
    @Component
    static class Programs implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;

        Programs(DomainSecurityService domainSecurityService) {
            this.domainSecurityService = domainSecurityService;
        }

        @Override
        public String type() {
            return ProgramSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return ProgramSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            if (domainSecurityService.isPlatformAdmin()) {
                return Optional.of(CatalogueSearchScopes.programs(true, Set.of()));
            }
            Set<UUID> ownIdentities = new HashSet<>();
            UUID courseCreatorUuid = domainSecurityService.getCurrentCourseCreatorUuid();
            if (courseCreatorUuid != null) {
                ownIdentities.add(courseCreatorUuid);
            }
            UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
            if (instructorUuid != null) {
                ownIdentities.add(instructorUuid);
            }
            return Optional.of(CatalogueSearchScopes.programs(false, ownIdentities));
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "title"),
                    GlobalSearchHit.text(document, "creator_name"),
                    null,
                    GlobalSearchHit.highlight(hit, "title", "category_name", "course_names", "creator_name", "description"));
        }
    }

    /**
     * {@code rubrics}: only for the people who build assessments - platform admins, course creators
     * (public rubrics plus their own) and instructors (public, active rubrics). Learners and anonymous
     * visitors never look for a rubric in a global search box, so the type is skipped for them.
     */
    @Component
    static class Rubrics implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;

        Rubrics(DomainSecurityService domainSecurityService) {
            this.domainSecurityService = domainSecurityService;
        }

        @Override
        public String type() {
            return RubricSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return RubricSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            if (domainSecurityService.isPlatformAdmin()) {
                return Optional.of(CatalogueSearchScopes.rubrics(true, null));
            }
            UUID courseCreatorUuid = domainSecurityService.getCurrentCourseCreatorUuid();
            if (courseCreatorUuid != null) {
                return Optional.of(CatalogueSearchScopes.rubrics(false, courseCreatorUuid));
            }
            if (domainSecurityService.getCurrentInstructorUuid() != null) {
                return Optional.of(CatalogueSearchScopes.publicRubrics());
            }
            return Optional.empty();
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "title"),
                    GlobalSearchHit.text(document, "rubric_type"),
                    null,
                    GlobalSearchHit.highlight(hit, "title", "description"));
        }
    }
}
