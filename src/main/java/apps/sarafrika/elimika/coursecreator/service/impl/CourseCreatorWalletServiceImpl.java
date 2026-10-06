package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorAchievementDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCompetencyDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorPortfolioItemDTO;
import apps.sarafrika.elimika.coursecreator.dto.WalletVerificationRequest;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorWalletService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.profile.spi.UserAchievementDTO;
import apps.sarafrika.elimika.profile.spi.UserCompetencyDTO;
import apps.sarafrika.elimika.profile.spi.UserPortfolioItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The course creator wallet sections, kept in the user-owned skills wallet shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorWalletServiceImpl implements CourseCreatorWalletService {

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorPortfolioItemDTO> listPortfolio(UUID courseCreatorUuid) {
        return profileService.portfolio().list(bridge.requireUserUuid(courseCreatorUuid)).stream()
                .map(item -> new CourseCreatorPortfolioItemDTO(item.uuid(), courseCreatorUuid, item.title(),
                        item.itemType(), item.linkUrl(), item.completedOn(), item.description(), item.createdDate()))
                .toList();
    }

    @Override
    public CourseCreatorPortfolioItemDTO savePortfolioItem(UUID courseCreatorUuid, UUID itemUuid,
                                                           CourseCreatorPortfolioItemDTO dto) {
        UUID userUuid = bridge.requireUserUuid(courseCreatorUuid);
        UserPortfolioItemDTO item = new UserPortfolioItemDTO(null, null, dto.title(), dto.itemType(), dto.linkUrl(),
                dto.completedOn(), dto.description(), null, null, null, null);
        UserPortfolioItemDTO saved = itemUuid == null
                ? profileService.portfolio().create(userUuid, item)
                : profileService.portfolio().update(userUuid, itemUuid, item);
        return new CourseCreatorPortfolioItemDTO(saved.uuid(), courseCreatorUuid, saved.title(), saved.itemType(),
                saved.linkUrl(), saved.completedOn(), saved.description(), saved.createdDate());
    }

    @Override
    public void deletePortfolioItem(UUID courseCreatorUuid, UUID itemUuid) {
        profileService.portfolio().delete(bridge.requireUserUuid(courseCreatorUuid), itemUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorCompetencyDTO> listCompetencies(UUID courseCreatorUuid) {
        return profileService.competencies().list(bridge.requireUserUuid(courseCreatorUuid)).stream()
                .map(item -> toDto(item, courseCreatorUuid))
                .toList();
    }

    @Override
    public CourseCreatorCompetencyDTO saveCompetency(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorCompetencyDTO dto) {
        UUID userUuid = bridge.requireUserUuid(courseCreatorUuid);
        UserCompetencyDTO item = new UserCompetencyDTO(null, null, dto.competency(), dto.framework(), dto.level(),
                dto.evidence(), null, null, null, null, null, null, null);
        UserCompetencyDTO saved = itemUuid == null
                ? profileService.competencies().create(userUuid, item)
                : profileService.competencies().update(userUuid, itemUuid, item);
        return toDto(saved, courseCreatorUuid);
    }

    @Override
    public void deleteCompetency(UUID courseCreatorUuid, UUID itemUuid) {
        profileService.competencies().delete(bridge.requireUserUuid(courseCreatorUuid), itemUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorAchievementDTO> listAchievements(UUID courseCreatorUuid) {
        return profileService.achievements().list(bridge.requireUserUuid(courseCreatorUuid)).stream()
                .map(item -> toDto(item, courseCreatorUuid))
                .toList();
    }

    @Override
    public CourseCreatorAchievementDTO saveAchievement(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorAchievementDTO dto) {
        UUID userUuid = bridge.requireUserUuid(courseCreatorUuid);
        UserAchievementDTO item = new UserAchievementDTO(null, null, dto.title(), dto.achievementType(),
                dto.awardedBy(), dto.awardedOn(), dto.description(), null, null, null, null);
        UserAchievementDTO saved = itemUuid == null
                ? profileService.achievements().create(userUuid, item)
                : profileService.achievements().update(userUuid, itemUuid, item);
        return toDto(saved, courseCreatorUuid);
    }

    @Override
    public void deleteAchievement(UUID courseCreatorUuid, UUID itemUuid) {
        profileService.achievements().delete(bridge.requireUserUuid(courseCreatorUuid), itemUuid);
    }

    /** The verdict lands on the shared item, so it holds for every domain of its owner. */
    @Override
    public void verifyItem(UUID courseCreatorUuid, String section, UUID itemUuid, WalletVerificationRequest request) {
        profileService.verify(bridge.requireUserUuid(courseCreatorUuid), ProfileSection.fromPath(section), itemUuid,
                new ProfileVerificationRequest(request.status(), request.notes()));
    }

    private static CourseCreatorCompetencyDTO toDto(UserCompetencyDTO item, UUID courseCreatorUuid) {
        return new CourseCreatorCompetencyDTO(item.uuid(), courseCreatorUuid, item.competency(), item.framework(),
                item.level(), item.evidence(), item.verificationStatus(), item.verifiedAt(), item.verificationNotes(),
                item.createdDate());
    }

    private static CourseCreatorAchievementDTO toDto(UserAchievementDTO item, UUID courseCreatorUuid) {
        return new CourseCreatorAchievementDTO(item.uuid(), courseCreatorUuid, item.title(), item.achievementType(),
                item.awardedBy(), item.awardedOn(), item.description(), item.createdDate());
    }
}
