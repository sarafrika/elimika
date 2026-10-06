package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorAchievementDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCompetencyDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorPortfolioItemDTO;
import apps.sarafrika.elimika.coursecreator.dto.WalletVerificationRequest;

import java.util.List;
import java.util.UUID;

/** The portfolio, competency and achievement tabs of the skills wallet, plus admin verification of items. */
public interface CourseCreatorWalletService {

    List<CourseCreatorPortfolioItemDTO> listPortfolio(UUID courseCreatorUuid);

    CourseCreatorPortfolioItemDTO savePortfolioItem(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorPortfolioItemDTO dto);

    void deletePortfolioItem(UUID courseCreatorUuid, UUID itemUuid);

    List<CourseCreatorCompetencyDTO> listCompetencies(UUID courseCreatorUuid);

    CourseCreatorCompetencyDTO saveCompetency(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorCompetencyDTO dto);

    void deleteCompetency(UUID courseCreatorUuid, UUID itemUuid);

    List<CourseCreatorAchievementDTO> listAchievements(UUID courseCreatorUuid);

    CourseCreatorAchievementDTO saveAchievement(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorAchievementDTO dto);

    void deleteAchievement(UUID courseCreatorUuid, UUID itemUuid);

    /** Admin check of a skill, competency or certification; section is skills, competencies or certifications. */
    void verifyItem(UUID courseCreatorUuid, String section, UUID itemUuid, WalletVerificationRequest request);
}
