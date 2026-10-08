package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.course.dto.ApplyCatalogueApplication;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueItem;
import apps.sarafrika.elimika.course.dto.ApplyCatalogueResponse;
import apps.sarafrika.elimika.course.internal.search.ApplyCatalogueSearchService.Caller;
import apps.sarafrika.elimika.course.internal.search.ApplyCatalogueSearchService.FacetSlot;
import apps.sarafrika.elimika.course.internal.search.ApplyCatalogueSearchService.Fit;
import apps.sarafrika.elimika.course.internal.search.ApplyCatalogueSearchService.Index;
import apps.sarafrika.elimika.course.internal.search.ApplyCatalogueSearchService.Query;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Show;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.search.FederatedSearchResult;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyCatalogueSearchServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID SKILL = UUID.randomUUID();
    private static final UUID APPLIED_COURSE = UUID.randomUUID();
    private static final UUID APPLIED_PROGRAM = UUID.randomUUID();
    private static final UUID APPLICATION = UUID.randomUUID();

    private final SearchAvailability availability = mock(SearchAvailability.class);
    private final SearchGateway gateway = mock(SearchGateway.class);
    private final ApplyCatalogueRecords records = mock(ApplyCatalogueRecords.class);
    private final DomainSecurityService security = mock(DomainSecurityService.class);
    private final InstructorLookupService instructors = mock(InstructorLookupService.class);
    private final ProfessionalProfileService profiles = mock(ProfessionalProfileService.class);
    @SuppressWarnings("unchecked")
    private final ProfileSectionService<UserSkillDTO> skills = mock(ProfileSectionService.class);

    private ApplyCatalogueSearchService service;

    @BeforeEach
    void setUp() {
        service = new ApplyCatalogueSearchService(availability, gateway, records, security, instructors, profiles);
        when(availability.isReadEnabled(any())).thenReturn(true);
        when(security.getCurrentUserUuid()).thenReturn(USER);
        when(instructors.findInstructorUuidByUserUuid(USER)).thenReturn(Optional.of(INSTRUCTOR));
        when(profiles.skills()).thenReturn(skills);
        // A free-text skill (no taxonomy entry) must be ignored.
        when(skills.list(USER)).thenReturn(List.of(skill(SKILL), skill(null)));
        when(records.courseApplications(INSTRUCTOR))
                .thenReturn(Map.of(APPLIED_COURSE, new ApplyCatalogueApplication(APPLICATION, "pending")));
        when(records.programApplications(INSTRUCTOR))
                .thenReturn(Map.of(APPLIED_PROGRAM, new ApplyCatalogueApplication(UUID.randomUUID(), "approved")));
        when(records.categoryNames(anyCollection())).thenReturn(Map.of());
    }

    // ===== Filter building =====

    @Test
    void appliedMatchesTheCallersApplicationsPerIndex() {
        Caller caller = caller(Set.of(SKILL));

        assertThat(ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller, Fit.APPLIED))
                .isEqualTo(SearchFilter.in("uuid", Set.of(APPLIED_COURSE)));
        assertThat(ApplyCatalogueSearchService.fitFilter(Index.PROGRAMMES, caller, Fit.APPLIED))
                .isEqualTo(SearchFilter.in("uuid", Set.of(APPLIED_PROGRAM)));
    }

    @Test
    void appliedWithoutApplicationsMatchesNothing() {
        Caller caller = new Caller(Set.of(), Map.of(), Map.of());

        assertThat(ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller, Fit.APPLIED))
                .isEqualTo(SearchFilter.in("uuid", List.of(ApplyCatalogueSearchService.NO_MATCH)));
        assertThat(ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller, Fit.OPEN)).isNull();
    }

    @Test
    void openExcludesAppliedAndSkillsAlsoRequiresAWalletSkill() {
        Caller caller = caller(Set.of(SKILL));

        SearchFilter notApplied = SearchFilter.notIn("uuid", Set.of(APPLIED_COURSE));
        assertThat(ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller, Fit.OPEN)).isEqualTo(notApplied);
        assertThat(ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller, Fit.SKILLS))
                .isEqualTo(SearchFilter.and(SearchFilter.in("skill_uuids", Set.of(SKILL)), notApplied));
    }

    @Test
    void skillsWithAnEmptyWalletMatchesNothing() {
        Caller caller = caller(Set.of());

        assertThat(ApplyCatalogueSearchService.fitFilter(Index.PROGRAMMES, caller, Fit.SKILLS))
                .isEqualTo(SearchFilter.and(SearchFilter.in("skill_uuids", List.of(ApplyCatalogueSearchService.NO_MATCH)),
                        SearchFilter.notIn("uuid", Set.of(APPLIED_PROGRAM))));
    }

    @Test
    void facetRequestsCountEveryFitAndAlwaysCarryThePublicScope() {
        UUID category = UUID.randomUUID();
        Query query = new Query(null, Show.ALL, List.of(category), Fit.SKILLS, null, 0, 24);
        List<FacetSlot> slots = new ArrayList<>();

        List<SearchRequest> requests = ApplyCatalogueSearchService.facetRequests(query, caller(Set.of(SKILL)), slots);

        // Per index: base, category (selection left out), and the two other fits.
        assertThat(requests).hasSize(8).hasSameSizeAs(slots);
        assertPublicScope(requests);
        SearchRequest courseCategory = requests.get(1);
        assertThat(courseCategory.facets()).containsExactly("category_uuids");
        assertThat(courseCategory.filter()).isEqualTo(
                ApplyCatalogueSearchService.fitFilter(Index.COURSES, caller(Set.of(SKILL)), Fit.SKILLS));
        assertThat(slots).extracting(FacetSlot::fit).containsExactly(
                Fit.SKILLS, Fit.SKILLS, Fit.OPEN, Fit.APPLIED, Fit.SKILLS, Fit.SKILLS, Fit.OPEN, Fit.APPLIED);
        SearchRequest programApplied = requests.get(7);
        assertThat(programApplied.filter()).isEqualTo(SearchFilter.and(
                SearchFilter.in("category_uuid", List.of(category)), SearchFilter.in("uuid", Set.of(APPLIED_PROGRAM))));
    }

    @Test
    void withoutACategoryTheBaseQueryFacetsCategories() {
        Query query = new Query("maths", Show.COURSES, List.of(), null, null, 0, 24);

        List<SearchRequest> requests = ApplyCatalogueSearchService.facetRequests(query, caller(Set.of()), new ArrayList<>());

        assertThat(requests).hasSize(6);
        assertThat(requests.getFirst().facets()).containsExactly("category_uuids");
        assertThat(requests.get(3).facets()).containsExactly("category_uuid");
        assertThat(requests).allSatisfy(request -> assertThat(request.text()).isEqualTo("maths"));
        assertPublicScope(requests);
    }

    // ===== Search and hydration =====

    @Test
    void hydratesTheCallersApplicationFeesAndSkillMatchAndRestatesTheTotal() {
        UUID openCourse = UUID.randomUUID();
        UUID hiddenProgram = UUID.randomUUID();
        when(gateway.multiSearchPerIndex(anyList())).thenAnswer(invocation -> {
            List<SearchRequest> requests = invocation.getArgument(0);
            return requests.stream().map(request -> new SearchPage(List.of(), 5, 0, 1, Map.of())).toList();
        });
        when(gateway.federatedSearch(anyList(), anyInt(), anyInt())).thenReturn(new FederatedSearchResult(List.of(
                new FederatedSearchResult.Hit("courses", APPLIED_COURSE, courseDocument(APPLIED_COURSE, List.of()), null, 1.0),
                new FederatedSearchResult.Hit("courses", openCourse, courseDocument(openCourse, List.of(SKILL.toString())), null, 0.9),
                new FederatedSearchResult.Hit("programs", hiddenProgram, Map.of("title", "Gone"), null, 0.5)), 3));
        when(records.publicCourseFees(anyCollection())).thenReturn(Map.of(
                APPLIED_COURSE, new BigDecimal("1500.00"), openCourse, BigDecimal.ZERO));
        when(records.publicProgramFees(anyCollection())).thenReturn(Map.of());

        ApplyCatalogueResponse response = service.search(new Query(null, null, List.of(), Fit.OPEN, null, 0, 24));

        assertThat(response.content()).extracting(ApplyCatalogueItem::uuid).containsExactly(APPLIED_COURSE, openCourse);
        ApplyCatalogueItem applied = response.content().getFirst();
        assertThat(applied.myApplication()).isEqualTo(new ApplyCatalogueApplication(APPLICATION, "pending"));
        assertThat(applied.minimumTrainingFee()).isEqualByComparingTo("1500.00");
        assertThat(applied.matchesSkills()).isFalse();
        assertThat(applied.lessonCount()).isEqualTo(7);
        assertThat(applied.requirementCount()).isEqualTo(2);
        assertThat(applied.code()).isEqualTo("C-1");
        assertThat(applied.courseCount()).isNull();
        ApplyCatalogueItem open = response.content().get(1);
        assertThat(open.myApplication()).isNull();
        assertThat(open.matchesSkills()).isTrue();
        // A hit failed the SQL re-check: the total is restated from what is shown.
        assertThat(response.metadata().getTotalElements()).isEqualTo(2);
        // Two shown indexes, five per fit query each.
        assertThat(response.facets().fit().open()).isEqualTo(10);
        assertThat(response.facets().fit().skills()).isEqualTo(10);
        assertThat(response.facets().fit().applied()).isEqualTo(10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SearchRequest>> facetCaptor = ArgumentCaptor.forClass(List.class);
        verify(gateway).multiSearchPerIndex(facetCaptor.capture());
        assertPublicScope(facetCaptor.getValue());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SearchRequest>> hitCaptor = ArgumentCaptor.forClass(List.class);
        verify(gateway).federatedSearch(hitCaptor.capture(), anyInt(), anyInt());
        assertPublicScope(hitCaptor.getValue());
        assertThat(hitCaptor.getValue().getFirst().filter()).isEqualTo(SearchFilter.notIn("uuid", Set.of(APPLIED_COURSE)));
    }

    @Test
    void searchUnavailableFailsWithoutAFallback() {
        when(availability.isReadEnabled("programs")).thenReturn(false);

        assertThatThrownBy(() -> service.search(new Query(null, null, null, null, null, 0, 24)))
                .isInstanceOf(SearchUnavailableException.class);
    }

    // ===== Helpers =====

    private static Caller caller(Set<UUID> skillUuids) {
        return new Caller(skillUuids,
                Map.of(APPLIED_COURSE, new ApplyCatalogueApplication(APPLICATION, "pending")),
                Map.of(APPLIED_PROGRAM, new ApplyCatalogueApplication(UUID.randomUUID(), "approved")));
    }

    private static void assertPublicScope(Collection<SearchRequest> requests) {
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.scope().isUnrestricted()).isFalse();
            SearchFilter expected = request.index().equals("courses")
                    ? CatalogueSearchScopes.courses(false, null, Set.of()).filter()
                    : CatalogueSearchScopes.programs(false, Set.of()).filter();
            assertThat(request.scope().filter()).isEqualTo(expected);
        });
    }

    private static Map<String, Object> courseDocument(UUID uuid, List<String> skillUuids) {
        Map<String, Object> document = new HashMap<>();
        document.put("uuid", uuid.toString());
        document.put("name", "Course " + uuid);
        document.put("course_code", "C-1");
        document.put("category_names", List.of("Maths"));
        document.put("lesson_count", 7);
        document.put("requirement_count", 2);
        document.put("skill_uuids", skillUuids);
        return document;
    }

    private static UserSkillDTO skill(UUID skillUuid) {
        return new UserSkillDTO(UUID.randomUUID(), USER, "Skill", skillUuid, null, null, null, null, null, null,
                null, null, null, null);
    }
}
