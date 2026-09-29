package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code instructors} in global search: the discovery half of {@link InstructorVisibility} - verified
 * instructors plus the caller's own profile, everything for a platform admin. Signed-in callers only:
 * the directory is not part of what an anonymous visitor can browse.
 */
@Component
@RequiredArgsConstructor
class InstructorGlobalSearchProvider implements GlobalSearchProvider {

    private final DomainSecurityService domainSecurityService;
    private final InstructorVisibility instructorVisibility;

    @Override
    public String type() {
        return InstructorSearchSource.INDEX;
    }

    @Override
    public SearchIndexDefinition definition() {
        return InstructorSearchSource.DEFINITION;
    }

    @Override
    public Optional<SearchScope> scopeForCurrentCaller() {
        if (domainSecurityService.getCurrentUserUuid() == null) {
            return Optional.empty();
        }
        InstructorVisibility.Caller caller = instructorVisibility.currentCaller();
        return Optional.of(caller.platformAdmin()
                ? SearchScope.unrestricted("platform-admin")
                : InstructorSearchScopes.forCaller(caller.ownInstructorUuid(), Set.of()));
    }

    @Override
    public GlobalSearchHit toHit(SearchHit hit) {
        Map<String, Object> document = hit.document();
        return new GlobalSearchHit(type(), hit.uuid(),
                GlobalSearchHit.text(document, "full_name"),
                GlobalSearchHit.text(document, "professional_headline"),
                null,
                GlobalSearchHit.highlight(hit, "full_name", "professional_headline", "skills", "bio"));
    }
}
