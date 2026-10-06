package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorAchievementDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCompetencyDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorPortfolioItemDTO;
import apps.sarafrika.elimika.coursecreator.dto.WalletVerificationRequest;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorAchievement;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCertification;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCompetency;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorPortfolioItem;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorSkill;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorAchievementRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCertificationRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorCompetencyRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorPortfolioItemRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorSkillRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorWalletService;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorWalletServiceImpl implements CourseCreatorWalletService {

    private final CourseCreatorRepository courseCreatorRepository;
    private final CourseCreatorPortfolioItemRepository portfolioRepository;
    private final CourseCreatorCompetencyRepository competencyRepository;
    private final CourseCreatorAchievementRepository achievementRepository;
    private final CourseCreatorSkillRepository skillRepository;
    private final CourseCreatorCertificationRepository certificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorPortfolioItemDTO> listPortfolio(UUID courseCreatorUuid) {
        return portfolioRepository.findByCourseCreatorUuidOrderByCreatedDateAsc(courseCreatorUuid).stream()
                .map(CourseCreatorWalletServiceImpl::toDTO).toList();
    }

    @Override
    public CourseCreatorPortfolioItemDTO savePortfolioItem(UUID courseCreatorUuid, UUID itemUuid,
                                                           CourseCreatorPortfolioItemDTO dto) {
        CourseCreatorPortfolioItem item = itemUuid == null
                ? newItem(courseCreatorUuid, new CourseCreatorPortfolioItem())
                : portfolioRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                        .orElseThrow(() -> notFound("Portfolio item", itemUuid));
        item.setTitle(dto.title());
        item.setItemType(dto.itemType());
        item.setLinkUrl(dto.linkUrl());
        item.setCompletedOn(dto.completedOn());
        item.setDescription(dto.description());
        return toDTO(portfolioRepository.save(item));
    }

    @Override
    public void deletePortfolioItem(UUID courseCreatorUuid, UUID itemUuid) {
        portfolioRepository.delete(portfolioRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                .orElseThrow(() -> notFound("Portfolio item", itemUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorCompetencyDTO> listCompetencies(UUID courseCreatorUuid) {
        return competencyRepository.findByCourseCreatorUuidOrderByCreatedDateAsc(courseCreatorUuid).stream()
                .map(CourseCreatorWalletServiceImpl::toDTO).toList();
    }

    @Override
    public CourseCreatorCompetencyDTO saveCompetency(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorCompetencyDTO dto) {
        CourseCreatorCompetency item = itemUuid == null
                ? newItem(courseCreatorUuid, new CourseCreatorCompetency())
                : competencyRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                        .orElseThrow(() -> notFound("Competency", itemUuid));
        boolean evidenceChanged = item.getUuid() != null && !java.util.Objects.equals(item.getEvidence(), dto.evidence());
        item.setCompetency(dto.competency());
        item.setFramework(dto.framework());
        item.setLevel(dto.level());
        item.setEvidence(dto.evidence());
        if (evidenceChanged) {
            // New evidence has not been checked, so the item goes back for verification.
            resetVerification(item);
        }
        return toDTO(competencyRepository.save(item));
    }

    @Override
    public void deleteCompetency(UUID courseCreatorUuid, UUID itemUuid) {
        competencyRepository.delete(competencyRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                .orElseThrow(() -> notFound("Competency", itemUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorAchievementDTO> listAchievements(UUID courseCreatorUuid) {
        return achievementRepository.findByCourseCreatorUuidOrderByCreatedDateAsc(courseCreatorUuid).stream()
                .map(CourseCreatorWalletServiceImpl::toDTO).toList();
    }

    @Override
    public CourseCreatorAchievementDTO saveAchievement(UUID courseCreatorUuid, UUID itemUuid, CourseCreatorAchievementDTO dto) {
        CourseCreatorAchievement item = itemUuid == null
                ? newItem(courseCreatorUuid, new CourseCreatorAchievement())
                : achievementRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                        .orElseThrow(() -> notFound("Achievement", itemUuid));
        item.setTitle(dto.title());
        item.setAchievementType(dto.achievementType());
        item.setAwardedBy(dto.awardedBy());
        item.setAwardedOn(dto.awardedOn());
        item.setDescription(dto.description());
        return toDTO(achievementRepository.save(item));
    }

    @Override
    public void deleteAchievement(UUID courseCreatorUuid, UUID itemUuid) {
        achievementRepository.delete(achievementRepository.findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                .orElseThrow(() -> notFound("Achievement", itemUuid)));
    }

    @Override
    public void verifyItem(UUID courseCreatorUuid, String section, UUID itemUuid, WalletVerificationRequest request) {
        if (request.status() == WalletVerificationStatus.PENDING) {
            throw new IllegalArgumentException("A verification must mark the item VERIFIED or REJECTED");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        switch (section == null ? "" : section.trim().toLowerCase(Locale.ROOT)) {
            case "skills" -> {
                CourseCreatorSkill skill = skillRepository.findByUuid(itemUuid)
                        .filter(found -> courseCreatorUuid.equals(found.getCourseCreatorUuid()))
                        .orElseThrow(() -> notFound("Skill", itemUuid));
                skill.setVerificationStatus(request.status());
                skill.setVerifiedAt(now);
                skill.setVerificationNotes(request.notes());
                skillRepository.save(skill);
            }
            case "competencies" -> {
                CourseCreatorCompetency competency = competencyRepository
                        .findByUuidAndCourseCreatorUuid(itemUuid, courseCreatorUuid)
                        .orElseThrow(() -> notFound("Competency", itemUuid));
                competency.setVerificationStatus(request.status());
                competency.setVerifiedAt(now);
                competency.setVerificationNotes(request.notes());
                competencyRepository.save(competency);
            }
            case "certifications" -> {
                CourseCreatorCertification certification = certificationRepository.findByUuid(itemUuid)
                        .filter(found -> courseCreatorUuid.equals(found.getCourseCreatorUuid()))
                        .orElseThrow(() -> notFound("Certification", itemUuid));
                certification.setIsVerified(request.status() == WalletVerificationStatus.VERIFIED);
                certificationRepository.save(certification);
            }
            default -> throw new IllegalArgumentException("Unknown wallet section: " + section);
        }
    }

    private <T> T newItem(UUID courseCreatorUuid, T item) {
        if (!courseCreatorRepository.existsByUuid(courseCreatorUuid)) {
            throw notFound("Course creator", courseCreatorUuid);
        }
        switch (item) {
            case CourseCreatorPortfolioItem portfolio -> portfolio.setCourseCreatorUuid(courseCreatorUuid);
            case CourseCreatorCompetency competency -> competency.setCourseCreatorUuid(courseCreatorUuid);
            case CourseCreatorAchievement achievement -> achievement.setCourseCreatorUuid(courseCreatorUuid);
            default -> throw new IllegalStateException("Unsupported wallet item " + item.getClass());
        }
        return item;
    }

    private static void resetVerification(CourseCreatorCompetency competency) {
        competency.setVerificationStatus(WalletVerificationStatus.PENDING);
        competency.setVerifiedAt(null);
        competency.setVerificationNotes(null);
    }

    private static ResourceNotFoundException notFound(String what, UUID uuid) {
        return new ResourceNotFoundException(what + " not found for UUID: " + uuid);
    }

    private static CourseCreatorPortfolioItemDTO toDTO(CourseCreatorPortfolioItem item) {
        return new CourseCreatorPortfolioItemDTO(item.getUuid(), item.getCourseCreatorUuid(), item.getTitle(),
                item.getItemType(), item.getLinkUrl(), item.getCompletedOn(), item.getDescription(), item.getCreatedDate());
    }

    private static CourseCreatorCompetencyDTO toDTO(CourseCreatorCompetency item) {
        return new CourseCreatorCompetencyDTO(item.getUuid(), item.getCourseCreatorUuid(), item.getCompetency(),
                item.getFramework(), item.getLevel(), item.getEvidence(), item.getVerificationStatus(),
                item.getVerifiedAt(), item.getVerificationNotes(), item.getCreatedDate());
    }

    private static CourseCreatorAchievementDTO toDTO(CourseCreatorAchievement item) {
        return new CourseCreatorAchievementDTO(item.getUuid(), item.getCourseCreatorUuid(), item.getTitle(),
                item.getAchievementType(), item.getAwardedBy(), item.getAwardedOn(), item.getDescription(),
                item.getCreatedDate());
    }
}
