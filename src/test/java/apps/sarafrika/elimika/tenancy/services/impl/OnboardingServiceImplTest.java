package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorOnboardingSteps;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCategoryPreference;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCategoryPreferenceRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.instructor.internal.InstructorOnboardingSteps;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.profile.internal.service.ProfileOnboardingSteps;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSummaryDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStepProvider;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingSummaryDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.internal.onboarding.AccountOnboardingSteps;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import apps.sarafrika.elimika.tenancy.util.enums.OnboardingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Generic onboarding orchestrator")
class OnboardingServiceImplTest {

    @Mock private DomainApprovalService domainApprovalService;
    @Mock private UserRepository userRepository;
    @Mock private ProfessionalProfileService profileService;
    @Mock private CourseCreatorRepository courseCreatorRepository;
    @Mock private CourseCreatorCategoryPreferenceRepository categoryRepository;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private LearnerProfileLookupService learnerLookupService;

    private final UUID userUuid = UUID.randomUUID();
    private final UUID creatorUuid = UUID.randomUUID();
    private final Map<UserDomain, DomainApplicationDTO> applications = new EnumMap<>(UserDomain.class);
    private CourseCreator creator;
    private OnboardingServiceImpl service;

    @BeforeEach
    void setUp() {
        List<OnboardingStepProvider> providers = List.of(
                new CourseCreatorOnboardingSteps(courseCreatorRepository, categoryRepository),
                new AccountOnboardingSteps(userRepository),
                new ProfileOnboardingSteps(profileService),
                new InstructorOnboardingSteps(profileService));
        service = new OnboardingServiceImpl(domainApprovalService, providerOf(providers),
                singleton(instructorLookupService), singleton(courseCreatorLookupService), singleton(learnerLookupService));

        User user = new User();
        user.setUuid(userUuid);
        user.setFirstName("Amina");
        user.setLastName("Otieno");
        user.setEmail("amina@example.com");
        user.setPhoneNumber("+254700000000");
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));

        ProfessionalProfileDTO basics = new ProfessionalProfileDTO(userUuid, "Builds robots", "Robotics trainer", null,
                "Kisumu", null, null, null);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("skills", 2L);
        counts.put("education", 1L);
        when(profileService.getSummary(userUuid)).thenReturn(new ProfileSummaryDTO(userUuid, basics, counts, 0, 3, 10, 30));
        when(profileService.getBasics(userUuid)).thenReturn(basics);

        when(instructorLookupService.findInstructorUuidByUserUuid(userUuid)).thenReturn(Optional.of(UUID.randomUUID()));
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid)).thenReturn(Optional.of(creatorUuid));
        creator = new CourseCreator();
        creator.setUuid(creatorUuid);
        creator.setUserUuid(userUuid);
        creator.setVerificationStatus(CourseCreatorVerificationStatus.DRAFT);
        when(courseCreatorRepository.findByUserUuid(userUuid)).thenReturn(Optional.of(creator));
        when(categoryRepository.findByCourseCreatorUuid(creatorUuid)).thenReturn(List.of());

        when(domainApprovalService.requiresApproval(any())).thenAnswer(invocation ->
                invocation.getArgument(0) != UserDomain.student && invocation.getArgument(0) != UserDomain.parent);
        when(domainApprovalService.application(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(applications.get((UserDomain) invocation.getArgument(1))));
        when(domainApprovalService.applicationsFor(userUuid)).thenAnswer(invocation -> List.copyOf(applications.values()));
        when(domainApprovalService.recordSubmission(any(), any())).thenAnswer(invocation -> {
            UserDomain domain = invocation.getArgument(1);
            DomainApplicationDTO current = applications.get(domain);
            DomainApplicationDTO submitted = new DomainApplicationDTO(userUuid, domain.name(), current.status(),
                    current.requestedAt(), LocalDateTime.now(), null, null);
            applications.put(domain, submitted);
            return submitted;
        });
        applications.put(UserDomain.instructor, application(UserDomain.instructor, DomainApprovalStatus.APPROVED, null));
        applications.put(UserDomain.course_creator, application(UserDomain.course_creator, DomainApprovalStatus.PENDING, null));
    }

    @Test
    @DisplayName("an instructor adding the course creator domain finds the shared steps already done")
    void sharedStepsCarryOverToASecondDomain() {
        OnboardingDTO onboarding = service.onboarding(userUuid, UserDomain.course_creator);

        assertThat(onboarding.steps()).extracting(OnboardingStep::key)
                .containsExactly("account", "professional_profile", "skills_wallet", "categories");
        assertThat(onboarding.steps()).filteredOn(OnboardingStep::shared)
                .extracting(OnboardingStep::key)
                .containsExactly("account", "professional_profile", "skills_wallet");
        assertThat(onboarding.steps()).filteredOn(OnboardingStep::shared).allMatch(OnboardingStep::complete);
        assertThat(onboarding.steps().get(2).counts()).containsEntry("skills", 2L);
        assertThat(onboarding.steps().get(3).complete()).isFalse();
        assertThat(onboarding.steps().get(3).missing()).containsExactly("categories");
        assertThat(onboarding.status()).isEqualTo(OnboardingStatus.IN_PROGRESS);
        assertThat(onboarding.readyForSubmission()).isFalse();
        assertThat(onboarding.stepsCompleted()).isEqualTo(3);
    }

    @Test
    @DisplayName("lists every held domain with its own state")
    void listsHeldDomains() {
        List<OnboardingSummaryDTO> domains = service.domainsOf(userUuid);

        assertThat(domains).extracting(OnboardingSummaryDTO::domain).containsExactly("instructor", "course_creator");
        assertThat(domains.get(0).status()).isEqualTo(OnboardingStatus.APPROVED);
        assertThat(domains.get(0).active()).isTrue();
        assertThat(domains.get(1).status()).isEqualTo(OnboardingStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("submit refuses while a required step is missing")
    void submitRequiresTheRequiredSteps() {
        assertThatThrownBy(() -> service.submit(userUuid, UserDomain.course_creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("categories");
        verify(domainApprovalService, never()).recordSubmission(any(), any());
        verify(courseCreatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit records the module state and the domain submission")
    void submitMarksTheDomainSubmitted() {
        CourseCreatorCategoryPreference preference = new CourseCreatorCategoryPreference();
        preference.setCourseCreatorUuid(creatorUuid);
        preference.setCategoryUuid(UUID.randomUUID());
        when(categoryRepository.findByCourseCreatorUuid(creatorUuid)).thenReturn(List.of(preference));

        OnboardingDTO submitted = service.submit(userUuid, UserDomain.course_creator);

        assertThat(submitted.status()).isEqualTo(OnboardingStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isNotNull();
        ArgumentCaptor<CourseCreator> saved = ArgumentCaptor.forClass(CourseCreator.class);
        verify(courseCreatorRepository).save(saved.capture());
        assertThat(saved.getValue().getVerificationStatus()).isEqualTo(CourseCreatorVerificationStatus.SUBMITTED);
        verify(domainApprovalService).recordSubmission(userUuid, UserDomain.course_creator);

        assertThatThrownBy(() -> service.submit(userUuid, UserDomain.course_creator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already awaiting review");
    }

    @Test
    @DisplayName("submit refuses an approved domain, an unrequested domain and the admin domain")
    void submitRefusals() {
        assertThatThrownBy(() -> service.submit(userUuid, UserDomain.instructor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already approved");
        assertThatThrownBy(() -> service.submit(userUuid, UserDomain.organisation_user))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.onboarding(userUuid, UserDomain.admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a domain without review is recorded as complete and stays active")
    void submitWithoutReview() {
        applications.put(UserDomain.parent, application(UserDomain.parent, DomainApprovalStatus.APPROVED, null));

        OnboardingDTO parent = service.submit(userUuid, UserDomain.parent);

        assertThat(parent.status()).isEqualTo(OnboardingStatus.APPROVED);
        assertThat(parent.active()).isTrue();
        assertThat(parent.steps()).extracting(OnboardingStep::key).containsExactly("account");
        verify(domainApprovalService).recordSubmission(userUuid, UserDomain.parent);
    }

    @Test
    @DisplayName("a domain not yet requested is a preview")
    void unrequestedDomainIsAPreview() {
        OnboardingDTO preview = service.onboarding(userUuid, UserDomain.organisation_user);

        assertThat(preview.requested()).isFalse();
        assertThat(preview.status()).isEqualTo(OnboardingStatus.NOT_STARTED);
        assertThat(preview.readyForSubmission()).isFalse();
    }

    @Test
    @DisplayName("status follows the mapping, and only non-account progress counts as started")
    void statusDerivation() {
        OnboardingStep account = OnboardingStep.of(10, "account", "Account", true, true, List.of());
        OnboardingStep categories = OnboardingStep.of(40, "categories", "Categories", true, false, List.of("categories"));
        DomainApplicationDTO pending = application(UserDomain.course_creator, DomainApprovalStatus.PENDING, null);

        assertThat(OnboardingServiceImpl.status(Optional.of(pending), List.of(account, categories)))
                .isEqualTo(OnboardingStatus.NOT_STARTED);
        assertThat(OnboardingServiceImpl.status(Optional.of(application(UserDomain.course_creator,
                DomainApprovalStatus.PENDING, LocalDateTime.now())), List.of(account))).isEqualTo(OnboardingStatus.SUBMITTED);
        assertThat(OnboardingServiceImpl.status(Optional.of(application(UserDomain.course_creator,
                DomainApprovalStatus.REJECTED, LocalDateTime.now())), List.of(account))).isEqualTo(OnboardingStatus.REJECTED);
        assertThat(OnboardingServiceImpl.status(Optional.of(application(UserDomain.course_creator,
                DomainApprovalStatus.SUSPENDED, null)), List.of(account))).isEqualTo(OnboardingStatus.SUSPENDED);
    }

    private DomainApplicationDTO application(UserDomain domain, DomainApprovalStatus status, LocalDateTime submittedAt) {
        return new DomainApplicationDTO(userUuid, domain.name(), status, LocalDateTime.now().minusDays(1), submittedAt,
                null, null);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<OnboardingStepProvider> providerOf(List<OnboardingStepProvider> providers) {
        ObjectProvider<OnboardingStepProvider> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(invocation -> providers.stream());
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> singleton(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(bean);
        return provider;
    }
}
