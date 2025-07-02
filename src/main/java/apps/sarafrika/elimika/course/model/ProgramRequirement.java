package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.RequirementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RequirementType requirementType;

    @Column(name = "requirement_text")
    private String requirementText;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;
}