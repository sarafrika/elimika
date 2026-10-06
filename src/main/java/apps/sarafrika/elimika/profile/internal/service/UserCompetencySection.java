package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserCompetency;
import apps.sarafrika.elimika.profile.internal.repository.UserCompetencyRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserCompetencyDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UserCompetencySection extends ProfileSectionSupport<UserCompetency, UserCompetencyDTO> {

    public UserCompetencySection(UserCompetencyRepository repository,
                                 GenericSpecificationBuilder<UserCompetency> specificationBuilder,
                                 ApplicationEventPublisher eventPublisher) {
        super(UserCompetency.class, ProfileSection.COMPETENCIES, repository, specificationBuilder, eventPublisher);
    }

    @Override
    protected UserCompetency newEntity() {
        return new UserCompetency();
    }

    @Override
    protected boolean apply(UserCompetency competency, UserCompetencyDTO dto) {
        boolean claimChanged = changed(competency.getCompetency(), dto.competency())
                || changed(competency.getEvidence(), dto.evidence());
        competency.setCompetency(dto.competency());
        competency.setFramework(dto.framework());
        competency.setLevel(dto.level());
        competency.setEvidence(dto.evidence());
        return claimChanged;
    }

    @Override
    protected String naturalKey(UserCompetencyDTO dto) {
        return key(dto.competency(), dto.framework());
    }

    @Override
    protected String naturalKeyOf(UserCompetency competency) {
        return key(competency.getCompetency(), competency.getFramework());
    }

    @Override
    protected UserCompetencyDTO toDto(UserCompetency c) {
        return new UserCompetencyDTO(c.getUuid(), c.getUserUuid(), c.getCompetency(), c.getFramework(), c.getLevel(),
                c.getEvidence(), c.getVerificationStatus(), c.getVerifiedAt(), c.getVerificationNotes(),
                c.getCreatedDate(), c.getCreatedBy(), c.getLastModifiedDate(), c.getLastModifiedBy());
    }
}
