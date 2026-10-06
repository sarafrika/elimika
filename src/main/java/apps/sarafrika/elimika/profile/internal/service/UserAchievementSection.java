package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserAchievement;
import apps.sarafrika.elimika.profile.internal.repository.UserAchievementRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserAchievementDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserAchievementSection extends ProfileSectionSupport<UserAchievement, UserAchievementDTO> {

    public UserAchievementSection(UserAchievementRepository repository,
                                  GenericSpecificationBuilder<UserAchievement> specificationBuilder,
                                  ApplicationEventPublisher eventPublisher) {
        super(UserAchievement.class, ProfileSection.ACHIEVEMENTS, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserAchievement newEntity() {
        return new UserAchievement();
    }

    @Override
    protected boolean apply(UserAchievement achievement, UserAchievementDTO dto) {
        achievement.setTitle(dto.title());
        achievement.setAchievementType(dto.achievementType());
        achievement.setAwardedBy(dto.awardedBy());
        achievement.setAwardedOn(dto.awardedOn());
        achievement.setDescription(dto.description());
        return false;
    }

    @Override
    protected String naturalKey(UserAchievementDTO dto) {
        return key(dto.title(), dto.awardedOn());
    }

    @Override
    protected String naturalKeyOf(UserAchievement achievement) {
        return key(achievement.getTitle(), achievement.getAwardedOn());
    }

    @Override
    protected UserAchievementDTO toDto(UserAchievement a) {
        return new UserAchievementDTO(a.getUuid(), a.getUserUuid(), a.getTitle(), a.getAchievementType(),
                a.getAwardedBy(), a.getAwardedOn(), a.getDescription(), a.getCreatedDate(), a.getCreatedBy(),
                a.getLastModifiedDate(), a.getLastModifiedBy());
    }
}
