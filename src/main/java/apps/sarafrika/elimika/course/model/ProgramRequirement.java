package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.RequirementTypeConverter;
import apps.sarafrika.elimika.course.util.enums.RequirementType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "program_requirements")
public class ProgramRequirement extends BaseEntity {

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "requirement_type")
    @Convert(converter = RequirementTypeConverter.class)
    private RequirementType requirementType;

    @Column(name = "requirement_text")
    private String requirementText;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;
}