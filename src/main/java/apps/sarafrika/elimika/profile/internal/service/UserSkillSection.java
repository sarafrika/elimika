package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserSkill;
import apps.sarafrika.elimika.profile.internal.repository.UserSkillRepository;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSkillSection extends ProfileSectionSupport<UserSkill, UserSkillDTO> {

    private final SkillLookupService skillLookupService;

    public UserSkillSection(UserSkillRepository repository, GenericSpecificationBuilder<UserSkill> specificationBuilder,
                            ApplicationEventPublisher eventPublisher, SkillLookupService skillLookupService) {
        super(UserSkill.class, ProfileSection.SKILLS, repository, specificationBuilder, eventPublisher);
        this.skillLookupService = skillLookupService;
    }

    @Override
    protected UserSkill newEntity() {
        return new UserSkill();
    }

    @Override
    protected boolean apply(UserSkill skill, UserSkillDTO dto) {
        boolean renamed = changed(skill.getSkillName(), dto.skillName());
        boolean claimChanged = renamed || changed(skill.getEvidence(), dto.evidence());
        skill.setSkillName(dto.skillName() == null ? null : dto.skillName().trim());
        skill.setProficiencyLevel(dto.proficiencyLevel() == null ? ProficiencyLevel.BEGINNER : dto.proficiencyLevel());
        skill.setEvidence(dto.evidence());
        skill.setLastAssessedOn(dto.lastAssessedOn());
        // A renamed skill is re-resolved; an unchanged one keeps its link unless it never had one.
        if (renamed || skill.getSkillUuid() == null) {
            skill.setSkillUuid(resolveTaxonomy(skill.getSkillName()));
        }
        return claimChanged;
    }

    @Override
    protected String naturalKey(UserSkillDTO dto) {
        return key(dto.skillName());
    }

    @Override
    protected String naturalKeyOf(UserSkill skill) {
        return key(skill.getSkillName());
    }

    @Override
    protected UserSkillDTO toDto(UserSkill skill) {
        return new UserSkillDTO(skill.getUuid(), skill.getUserUuid(), skill.getSkillName(), skill.getSkillUuid(),
                skill.getProficiencyLevel(), skill.getEvidence(), skill.getLastAssessedOn(),
                skill.getVerificationStatus(), skill.getVerifiedAt(), skill.getVerificationNotes(),
                skill.getCreatedDate(), skill.getCreatedBy(), skill.getLastModifiedDate(), skill.getLastModifiedBy());
    }

    private java.util.UUID resolveTaxonomy(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        SkillSummary match = skillLookupService.resolve(List.of(name)).get(name);
        return match == null ? null : match.uuid();
    }
}
