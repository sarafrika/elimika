package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserExperience;
import apps.sarafrika.elimika.profile.internal.repository.UserExperienceRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserExperienceSection extends ProfileSectionSupport<UserExperience, UserExperienceDTO> {

    public UserExperienceSection(UserExperienceRepository repository,
                                 GenericSpecificationBuilder<UserExperience> specificationBuilder,
                                 ApplicationEventPublisher eventPublisher) {
        super(UserExperience.class, ProfileSection.EXPERIENCE, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserExperience newEntity() {
        return new UserExperience();
    }

    @Override
    protected boolean apply(UserExperience experience, UserExperienceDTO dto) {
        experience.setPosition(dto.position());
        experience.setOrganizationName(dto.organizationName());
        experience.setResponsibilities(dto.responsibilities());
        experience.setYearsOfExperience(dto.yearsOfExperience());
        experience.setStartDate(dto.startDate());
        experience.setEndDate(dto.endDate());
        experience.setIsCurrentPosition(Boolean.TRUE.equals(dto.isCurrentPosition()));
        experience.setExperienceType(dto.experienceType());
        return false;
    }

    @Override
    protected String naturalKey(UserExperienceDTO dto) {
        return key(dto.position(), dto.organizationName(), dto.startDate());
    }

    @Override
    protected String naturalKeyOf(UserExperience experience) {
        return key(experience.getPosition(), experience.getOrganizationName(), experience.getStartDate());
    }

    @Override
    protected UserExperienceDTO toDto(UserExperience e) {
        return new UserExperienceDTO(e.getUuid(), e.getUserUuid(), e.getPosition(), e.getOrganizationName(),
                e.getResponsibilities(), e.getYearsOfExperience(), e.getStartDate(), e.getEndDate(),
                e.getIsCurrentPosition(), e.getExperienceType(), e.getCreatedDate(), e.getCreatedBy(),
                e.getLastModifiedDate(), e.getLastModifiedBy());
    }
}
