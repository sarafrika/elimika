package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCategoriesRequest;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCategoryPreferenceDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorOnboardingStateDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCategoryPreference;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCategoryPreferenceRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.event.user.ProfileModerationDecidedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubmissionService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseCreatorOnboardingServiceImplTest {

    @Mock
    private CourseCreatorRepository courseCreatorRepository;
    @Mock
    private CourseCreatorCategoryPreferenceRepository categoryPreferenceRepository;
    @Mock
    private CourseCreatorSkillsWallet skillsWallet;
    @Mock
    private UserContextService userContextService;
    @Mock
    private DomainSecurityService domainSecurityService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private OnboardingSubmissionService onboardingSubmissionService;

    private CourseCreatorOnboardingServiceImpl service;
    private final UUID userUuid = UUID.randomUUID();
    private final UUID creatorUuid = UUID.randomUUID();
    private CourseCreator creator;

    @BeforeEach
    void setUp() {
        service = new CourseCreatorOnboardingServiceImpl(courseCreatorRepository, categoryPreferenceRepository,
                skillsWallet, userContextService, domainSecurityService, applicationEventPublisher, onboardingSubmissionService);
        creator = new CourseCreator();
        creator.setUuid(creatorUuid);
        creator.setUserUuid(userUuid);
        creator.setAdminVerified(false);
        creator.setVerificationStatus(CourseCreatorVerificationStatus.DRAFT);
        when(userContextService.getCurrentUserUuid()).thenReturn(userUuid);
        when(courseCreatorRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(creator));
        when(courseCreatorRepository.findByUuid(creatorUuid)).thenReturn(Optional.of(creator));
        when(courseCreatorRepository.save(any(CourseCreator.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void readingOnboardingNeverCreatesAProfile() {
        when(courseCreatorRepository.findByUserUuid(userUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentOnboarding()).isInstanceOf(ResourceNotFoundException.class);
        verify(courseCreatorRepository, never()).save(any());
    }

    @Test
    void updateCategoriesPersistsDistinctCategoryUuids() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(categoryPreferenceRepository.findByCourseCreatorUuid(creatorUuid))
                .thenReturn(List.of(preference(first), preference(second)));

        CourseCreatorOnboardingStateDTO state = service.updateCategories(
                new CourseCreatorCategoriesRequest(List.of(first, first, second)));

        verify(categoryPreferenceRepository).deleteByCourseCreatorUuidAndCategoryUuidNotIn(creatorUuid, List.of(first, second));
        ArgumentCaptor<CourseCreatorCategoryPreference> saved = ArgumentCaptor.forClass(CourseCreatorCategoryPreference.class);
        verify(categoryPreferenceRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(CourseCreatorCategoryPreference::getCategoryUuid)
                .containsExactly(first, second);
        assertThat(state.categories()).extracting(CourseCreatorCategoryPreferenceDTO::categoryUuid)
                .containsExactly(first, second);
    }

    @Test
    void submitDelegatesToTheGenericOnboardingSubmit() {
        service.submitCurrentForVerification();

        verify(onboardingSubmissionService).submitOnboarding(userUuid, UserDomain.course_creator);
    }

    @Test
    void submitSurfacesTheGenericRefusal() {
        doThrow(new IllegalStateException("The course_creator onboarding is already awaiting review"))
                .when(onboardingSubmissionService).submitOnboarding(userUuid, UserDomain.course_creator);

        assertThatThrownBy(() -> service.submitCurrentForVerification())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already awaiting review");
    }

    @Test
    void approvalVerifiesTheProfileAndApprovesTheDomain() {
        creator.setVerificationStatus(CourseCreatorVerificationStatus.SUBMITTED);

        CourseCreatorOnboardingStateDTO state = service.moderate(creatorUuid, "approve", "Strong portfolio");

        assertThat(state.verificationStatus()).isEqualTo(CourseCreatorVerificationStatus.APPROVED);
        assertThat(state.adminVerified()).isTrue();
        ArgumentCaptor<ProfileModerationDecidedEvent> event = ArgumentCaptor.forClass(ProfileModerationDecidedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().status()).isEqualTo(DomainApprovalStatus.APPROVED);
        assertThat(event.getValue().userDomain()).isEqualTo("course_creator");
    }

    @Test
    void rejectionAndRevocationOnlyApplyToTheMatchingState() {
        assertThatThrownBy(() -> service.moderate(creatorUuid, "reject", null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.moderate(creatorUuid, "revoke", null)).isInstanceOf(IllegalStateException.class);

        creator.setVerificationStatus(CourseCreatorVerificationStatus.APPROVED);
        service.moderate(creatorUuid, "revoke", "Copied content");

        ArgumentCaptor<ProfileModerationDecidedEvent> event = ArgumentCaptor.forClass(ProfileModerationDecidedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().status()).isEqualTo(DomainApprovalStatus.SUSPENDED);
        assertThat(creator.getVerificationStatus()).isEqualTo(CourseCreatorVerificationStatus.REVOKED);
    }

    private CourseCreatorCategoryPreference preference(UUID categoryUuid) {
        CourseCreatorCategoryPreference preference = new CourseCreatorCategoryPreference();
        preference.setCourseCreatorUuid(creatorUuid);
        preference.setCategoryUuid(categoryUuid);
        return preference;
    }
}
