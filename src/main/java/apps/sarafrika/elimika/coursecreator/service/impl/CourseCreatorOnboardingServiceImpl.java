package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCategoriesRequest;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCategoryPreferenceDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorOnboardingStateDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCategoryPreference;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCategoryPreferenceRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorOnboardingService;
import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import apps.sarafrika.elimika.shared.event.user.ProfileModerationDecidedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingSubmissionService;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseCreatorOnboardingServiceImpl implements CourseCreatorOnboardingService {

    private final CourseCreatorRepository courseCreatorRepository;
    private final CourseCreatorCategoryPreferenceRepository categoryPreferenceRepository;
    private final CourseCreatorSkillsWallet skillsWallet;
    private final UserContextService userContextService;
    private final DomainSecurityService domainSecurityService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OnboardingSubmissionService onboardingSubmissionService;

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorOnboardingStateDTO getCurrentOnboarding() {
        return toState(currentCreator());
    }

    @Override
    public CourseCreatorOnboardingStateDTO updateCategories(CourseCreatorCategoriesRequest request) {
        CourseCreator creator = currentCreator();
        List<UUID> categoryUuids = distinctUuids(request.categoryUuids());

        if (categoryUuids.isEmpty()) {
            categoryPreferenceRepository.deleteByCourseCreatorUuid(creator.getUuid());
        } else {
            categoryPreferenceRepository.deleteByCourseCreatorUuidAndCategoryUuidNotIn(creator.getUuid(), categoryUuids);
            for (UUID categoryUuid : categoryUuids) {
                if (!categoryPreferenceRepository.existsByCourseCreatorUuidAndCategoryUuid(creator.getUuid(), categoryUuid)) {
                    CourseCreatorCategoryPreference preference = new CourseCreatorCategoryPreference();
                    preference.setCourseCreatorUuid(creator.getUuid());
                    preference.setCategoryUuid(categoryUuid);
                    categoryPreferenceRepository.save(preference);
                }
            }
        }

        return toState(creator);
    }

    @Override
    public CourseCreatorOnboardingStateDTO submitCurrentForVerification() {
        CourseCreator creator = currentCreator();
        // Same rules and side effects as POST /api/v1/onboarding/course_creator/submit.
        onboardingSubmissionService.submitOnboarding(creator.getUserUuid(), UserDomain.course_creator);
        return toState(currentCreator());
    }

    @Override
    public CourseCreatorOnboardingStateDTO moderate(UUID courseCreatorUuid, String action, String reason) {
        CourseCreator creator = courseCreatorRepository.findByUuid(courseCreatorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course creator not found for UUID: " + courseCreatorUuid));
        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        CourseCreatorVerificationStatus current = creator.getVerificationStatus();

        DomainApprovalStatus domainStatus = switch (normalizedAction) {
            case "approve" -> {
                domainSecurityService.enforceNotSelfApprovingProfile(creator.getUserUuid(), "course creator");
                applyDecision(creator, CourseCreatorVerificationStatus.APPROVED, true, reason);
                yield DomainApprovalStatus.APPROVED;
            }
            case "reject" -> {
                if (current != CourseCreatorVerificationStatus.SUBMITTED) {
                    throw new IllegalStateException("Only a submitted course creator profile can be rejected.");
                }
                applyDecision(creator, CourseCreatorVerificationStatus.REJECTED, false, reason);
                yield DomainApprovalStatus.REJECTED;
            }
            case "revoke" -> {
                if (current != CourseCreatorVerificationStatus.APPROVED) {
                    throw new IllegalStateException("Only an approved course creator profile can be revoked.");
                }
                applyDecision(creator, CourseCreatorVerificationStatus.REVOKED, false, reason);
                yield DomainApprovalStatus.SUSPENDED;
            }
            default -> throw new IllegalArgumentException("Unsupported moderation action: " + action);
        };

        CourseCreator saved = courseCreatorRepository.save(creator);
        // Tenancy applies the same decision to the course_creator domain and notifies the creator.
        applicationEventPublisher.publishEvent(new ProfileModerationDecidedEvent(saved.getUserUuid(),
                UserDomain.course_creator.name(), domainStatus, reason, domainSecurityService.getCurrentUserUuid()));
        return toState(saved);
    }

    private static void applyDecision(CourseCreator creator, CourseCreatorVerificationStatus status,
                                      boolean adminVerified, String reason) {
        creator.setAdminVerified(adminVerified);
        creator.setVerificationStatus(status);
        creator.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        creator.setReviewReason(reason);
    }

    private CourseCreator currentCreator() {
        UUID userUuid = userContextService.getCurrentUserUuid();
        return courseCreatorRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No course creator profile. Register as a course creator or apply for the domain first."));
    }

    private CourseCreatorOnboardingStateDTO toState(CourseCreator creator) {
        List<CourseCreatorCategoryPreferenceDTO> categories = categoriesFor(creator.getUuid());
        boolean readyForSubmission = !categories.isEmpty()
                && skillsWallet.hasSkills(creator.getUuid());

        return new CourseCreatorOnboardingStateDTO(
                creator.getUuid(),
                categories,
                skillsWallet.completedSections(creator.getUuid()),
                CourseCreatorSkillsWallet.SECTION_TOTAL,
                creator.getVerificationStatus(),
                creator.getAdminVerified(),
                creator.getVerificationRequestedAt(),
                creator.getSubmittedAt(),
                creator.getReviewedAt(),
                creator.getReviewReason(),
                readyForSubmission
        );
    }

    private List<CourseCreatorCategoryPreferenceDTO> categoriesFor(UUID courseCreatorUuid) {
        return categoryPreferenceRepository.findByCourseCreatorUuid(courseCreatorUuid).stream()
                .map(preference -> new CourseCreatorCategoryPreferenceDTO(preference.getCategoryUuid()))
                .toList();
    }

    private static List<UUID> distinctUuids(List<UUID> uuids) {
        if (uuids == null) {
            return List.of();
        }
        return uuids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
