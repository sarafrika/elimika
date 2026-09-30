package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.ClassListingVisibility;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The classes module's indexes in global search.
 */
public final class ClassesGlobalSearchProviders {

    private ClassesGlobalSearchProviders() {
    }

    /**
     * {@code classes}: the class listing's scope ({@link ClassSearchScopes#forGlobalSearch}). An
     * anonymous caller has no staffed organisation, instructor profile or enrolment, so they get active,
     * public classes whose content is approved.
     */
    @Component
    static class Classes implements GlobalSearchProvider {

        private final ClassListingVisibility classListingVisibility;
        private final ClassDefinitionRepository classDefinitionRepository;
        private final BranchLocationResolver branchLocationResolver;

        Classes(ClassListingVisibility classListingVisibility, ClassDefinitionRepository classDefinitionRepository,
                BranchLocationResolver branchLocationResolver) {
            this.classListingVisibility = classListingVisibility;
            this.classDefinitionRepository = classDefinitionRepository;
            this.branchLocationResolver = branchLocationResolver;
        }

        @Override
        public Map<UUID, SearchGeoPoint> nearMePoints(Collection<UUID> uuids) {
            Map<UUID, SearchGeoPoint> points = new HashMap<>();
            if (uuids == null || uuids.isEmpty()) {
                return points;
            }
            var branchPins = BranchLocationResolver.branchPinMemo();
            for (ClassDefinition definition : classDefinitionRepository.findByUuidIn(uuids)) {
                SearchGeoPoint point = branchLocationResolver.searchPoint(definition.getOrganisationUuid(),
                        definition.getBranchUuid(), definition.getLocationType(), definition.getLocationLatitude(),
                        definition.getLocationLongitude(), branchPins);
                if (point != null) {
                    points.put(definition.getUuid(), point);
                }
            }
            return points;
        }

        @Override
        public String type() {
            return ClassSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return ClassSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            return Optional.of(ClassSearchScopes.forGlobalSearch(classListingVisibility.forCurrentCaller()));
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            String subtitle = GlobalSearchHit.text(document, "organisation_name");
            if (subtitle == null) {
                subtitle = GlobalSearchHit.text(document, "instructor_name");
            }
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "title"),
                    subtitle,
                    null,
                    GlobalSearchHit.highlight(hit, "title", "course_name", "program_title", "organisation_name",
                            "instructor_name", "description"));
        }
    }

    /**
     * {@code marketplace_jobs}: signed-in callers only - the marketplace is where instructors look
     * for work, not something an anonymous visitor browses. A platform admin sees every job; everyone
     * else open jobs plus every job of the organisations they staff, the job listing's rule applied
     * to all the caller's organisations at once.
     */
    @Component
    static class MarketplaceJobs implements GlobalSearchProvider {

        private final DomainSecurityService domainSecurityService;
        private final ClassListingVisibility classListingVisibility;
        private final ClassMarketplaceJobRepository jobRepository;
        private final BranchLocationResolver branchLocationResolver;

        MarketplaceJobs(DomainSecurityService domainSecurityService, ClassListingVisibility classListingVisibility,
                        ClassMarketplaceJobRepository jobRepository, BranchLocationResolver branchLocationResolver) {
            this.domainSecurityService = domainSecurityService;
            this.classListingVisibility = classListingVisibility;
            this.jobRepository = jobRepository;
            this.branchLocationResolver = branchLocationResolver;
        }

        @Override
        public Map<UUID, SearchGeoPoint> nearMePoints(Collection<UUID> uuids) {
            Map<UUID, SearchGeoPoint> points = new HashMap<>();
            if (uuids == null || uuids.isEmpty()) {
                return points;
            }
            var branchPins = BranchLocationResolver.branchPinMemo();
            for (ClassMarketplaceJob job : jobRepository.findByUuidIn(uuids)) {
                SearchGeoPoint point = branchLocationResolver.searchPoint(job.getOrganisationUuid(),
                        job.getBranchUuid(), job.getLocationType(), job.getLocationLatitude(),
                        job.getLocationLongitude(), branchPins);
                if (point != null) {
                    points.put(job.getUuid(), point);
                }
            }
            return points;
        }

        @Override
        public String type() {
            return MarketplaceJobSearchSource.INDEX;
        }

        @Override
        public SearchIndexDefinition definition() {
            return MarketplaceJobSearchSource.DEFINITION;
        }

        @Override
        public Optional<SearchScope> scopeForCurrentCaller() {
            if (domainSecurityService.getCurrentUserUuid() == null) {
                return Optional.empty();
            }
            if (domainSecurityService.isPlatformAdmin()) {
                return Optional.of(MarketplaceJobSearchScopes.forCaller(true, null, false));
            }
            Set<UUID> staffed = classListingVisibility.staffedOrganisationsOfCurrentCaller();
            SearchFilter open = SearchFilter.eq("status", ClassMarketplaceJobStatus.OPEN.name());
            if (staffed.isEmpty()) {
                return Optional.of(SearchScope.of(open, "open-jobs"));
            }
            return Optional.of(SearchScope.of(SearchFilter.or(open, SearchFilter.in("organisation_uuid", staffed)),
                    "open-jobs+staffed:" + staffed.size()));
        }

        @Override
        public GlobalSearchHit toHit(SearchHit hit) {
            Map<String, Object> document = hit.document();
            return new GlobalSearchHit(type(), hit.uuid(),
                    GlobalSearchHit.text(document, "title"),
                    GlobalSearchHit.text(document, "organisation_name"),
                    null,
                    GlobalSearchHit.highlight(hit, "title", "course_name", "program_title", "organisation_name",
                            "description"));
        }
    }
}
