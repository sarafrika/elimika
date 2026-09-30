package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A skill a marketplace job asks of its instructor, tagged by the posting organisation. */
@Entity
@Table(name = "class_marketplace_job_required_skills")
@Getter
@Setter
@NoArgsConstructor
public class ClassMarketplaceJobRequiredSkill extends BaseEntity {

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "min_proficiency")
    @Convert(converter = ProficiencyLevelConverter.class)
    private ProficiencyLevel minProficiency;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    public ClassMarketplaceJobRequiredSkill(UUID jobUuid, UUID skillUuid, ProficiencyLevel minProficiency,
                                            Boolean isMandatory) {
        this.jobUuid = jobUuid;
        this.skillUuid = skillUuid;
        this.minProficiency = minProficiency;
        this.isMandatory = isMandatory;
    }
}
