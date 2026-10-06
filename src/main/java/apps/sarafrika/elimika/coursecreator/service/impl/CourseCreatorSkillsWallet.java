package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorAchievementRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCertificationRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCompetencyRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorExperienceRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorPortfolioItemRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorSkillRepository;
import apps.sarafrika.elimika.coursecreator.util.enums.WalletVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Counts the wallet tabs that have content; Verification counts once an admin verified any item. */
@Component
@RequiredArgsConstructor
class CourseCreatorSkillsWallet {

    static final int SECTION_TOTAL = 7;

    private final CourseCreatorSkillRepository skillRepository;
    private final CourseCreatorPortfolioItemRepository portfolioRepository;
    private final CourseCreatorCertificationRepository certificationRepository;
    private final CourseCreatorCompetencyRepository competencyRepository;
    private final CourseCreatorExperienceRepository experienceRepository;
    private final CourseCreatorAchievementRepository achievementRepository;

    int completedSections(UUID creatorUuid) {
        int completed = 0;
        completed += skillRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += portfolioRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += certificationRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += competencyRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += experienceRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += achievementRepository.countByCourseCreatorUuid(creatorUuid) > 0 ? 1 : 0;
        completed += anyVerified(creatorUuid) ? 1 : 0;
        return completed;
    }

    private boolean anyVerified(UUID creatorUuid) {
        return skillRepository.existsByCourseCreatorUuidAndVerificationStatus(creatorUuid, WalletVerificationStatus.VERIFIED)
                || competencyRepository.existsByCourseCreatorUuidAndVerificationStatus(creatorUuid, WalletVerificationStatus.VERIFIED)
                || certificationRepository.existsByCourseCreatorUuidAndIsVerifiedTrue(creatorUuid);
    }
}
