package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchProfile;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstructorMatchingServiceImplTest {

    private final InstructorRepository instructorRepository = mock(InstructorRepository.class);
    private final ProfessionalProfileService profileService = mock(ProfessionalProfileService.class);
    @SuppressWarnings("unchecked")
    private final ProfileSectionService<UserSkillDTO> skills = mock(ProfileSectionService.class);
    @SuppressWarnings("unchecked")
    private final ProfileSectionService<UserExperienceDTO> experience = mock(ProfileSectionService.class);
    private final InstructorReviewRepository reviewRepository = mock(InstructorReviewRepository.class);
    private final SearchAvailability searchAvailability = mock(SearchAvailability.class);
    private final SearchGateway searchGateway = mock(SearchGateway.class);
    private final InstructorMatchingServiceImpl service = new InstructorMatchingServiceImpl(instructorRepository,
            profileService, reviewRepository, searchAvailability, searchGateway);

    @Test
    void theBayesianRatingShrinksTowardsThePlatformMean() {
        assertThat(InstructorMatchingServiceImpl.bayesian(5.0, 1, 3.0)).isCloseTo((5 * 3.0 + 5.0) / 6, within(1e-9));
        assertThat(InstructorMatchingServiceImpl.bayesian(null, 0, 3.0)).isEqualTo(3.0);
        assertThat(InstructorMatchingServiceImpl.bayesian(null, 0, null)).isNull();
    }

    @Test
    void onlyAnOptedInVerifiedInstructorCarriesAPointAndOnlyLinkedSkillsCount() {
        Instructor optedIn = instructor(true, true);
        Instructor optedOut = instructor(false, true);
        UUID python = UUID.randomUUID();
        UserSkillDTO linked = skill(optedIn.getUserUuid(), "Python", python, ProficiencyLevel.ADVANCED);
        UserSkillDTO freeText = skill(optedIn.getUserUuid(), "Juggling", null, null);
        when(instructorRepository.findByUuidIn(anyCollection())).thenReturn(List.of(optedIn, optedOut));
        when(profileService.skills()).thenReturn(skills);
        when(profileService.experience()).thenReturn(experience);
        when(skills.listForUsers(anyCollection())).thenReturn(List.of(linked, freeText));
        when(experience.listForUsers(anyCollection())).thenReturn(List.of());

        Map<UUID, InstructorMatchProfile> profiles = service.findMatchProfiles(List.of(optedIn.getUuid(), optedOut.getUuid()));

        assertThat(profiles.get(optedIn.getUuid()).searchPoint()).isNotNull();
        assertThat(profiles.get(optedOut.getUuid()).searchPoint()).isNull();
        assertThat(profiles.get(optedIn.getUuid()).skillLevels()).containsOnly(Map.entry(python, ProficiencyLevel.ADVANCED));
    }

    @Test
    void candidatesAreSearchedAmongTheApprovedSetWithinTheVerifiedScopeGeoFirst() {
        UUID near = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        when(searchAvailability.isReadEnabled(InstructorSearchSource.INDEX)).thenReturn(true);
        when(searchGateway.search(any()))
                .thenReturn(page(near))
                .thenReturn(page(far, near));

        List<UUID> result = service.searchVerifiedAmong(List.of(near, far), new NearMe(-1.29, 36.82, 25), 10);

        assertThat(result).containsExactly(near, far);
        ArgumentCaptor<SearchRequest> requests = ArgumentCaptor.forClass(SearchRequest.class);
        verify(searchGateway, times(2)).search(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.scope().filter().toString()).contains("admin_verified");
            assertThat(request.filter().toString()).contains("uuid");
        });
        assertThat(requests.getAllValues().get(0).filter().toString()).contains("GeoRadius");
        assertThat(requests.getAllValues().get(1).filter().toString()).doesNotContain("GeoRadius");
    }

    private static SearchPage page(UUID... uuids) {
        return new SearchPage(java.util.Arrays.stream(uuids).map(uuid -> new SearchHit(uuid, Map.of(), Map.of())).toList(),
                uuids.length, 0, 100, Map.of());
    }

    private static UserSkillDTO skill(UUID userUuid, String name, UUID skillUuid, ProficiencyLevel level) {
        return new UserSkillDTO(UUID.randomUUID(), userUuid, name, skillUuid, level, null, null, null, null, null,
                null, null, null, null);
    }

    private static Instructor instructor(boolean optedIn, boolean verified) {
        Instructor instructor = new Instructor();
        instructor.setUuid(UUID.randomUUID());
        instructor.setUserUuid(UUID.randomUUID());
        instructor.setFullName("Jane Doe");
        instructor.setAdminVerified(verified);
        instructor.setLocationSearchOptIn(optedIn);
        instructor.setLatitude(new BigDecimal("-1.292066"));
        instructor.setLongitude(new BigDecimal("36.821945"));
        return instructor;
    }
}
