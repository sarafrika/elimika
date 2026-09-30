package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final InstructorRepository instructorRepository;

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
    public Map<UUID, SearchGeoPoint> nearMePoints(Collection<UUID> uuids) {
        Map<UUID, SearchGeoPoint> points = new HashMap<>();
        if (uuids == null || uuids.isEmpty()) {
            return points;
        }
        for (Instructor instructor : instructorRepository.findByUuidIn(uuids)) {
            SearchGeoPoint point = InstructorSearchSource.nearMePoint(instructor);
            if (point != null) {
                points.put(instructor.getUuid(), point);
            }
        }
        return points;
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
